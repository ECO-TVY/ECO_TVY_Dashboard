package com.ecotvy.infrastructure.security;

import com.ecotvy.application.port.out.UserRepositoryPort;
import com.ecotvy.domain.user.User;
import com.ecotvy.domain.user.UserRole;
import com.ecotvy.infrastructure.error.enums.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer {token} 헤더를 검증해 SecurityContext에 인증 정보를 넣는다.
 * principal은 userId(Long)이므로 컨트롤러에서 @AuthenticationPrincipal Long userId로 받을 수 있다.
 *
 * 토큰이 없거나 잘못된 경우 여기서 응답하지 않고 그대로 통과시킨다.
 * 보호된 경로라면 이후 RestAuthenticationEntryPoint가 401을 반환한다.
 *
 * SecurityConfig에서 직접 생성한다 (@Component로 등록하면 서블릿 필터로 중복 등록됨).
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** EntryPoint가 401 응답의 에러 코드를 고르기 위해 읽는 request attribute */
    public static final String AUTH_ERROR_ATTRIBUTE = "jwt.auth.error";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepositoryPort userRepositoryPort;
    private final SecurityErrorResponseWriter errorResponseWriter;

    private static boolean isAllowedBeforePasswordChange(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return ("PUT".equals(method) && "/api/users/me/password".equals(uri))
                || ("GET".equals(method) && "/api/users/me".equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                Long userId = jwtTokenProvider.getUserId(claims);
                UserRole role = jwtTokenProvider.getRole(claims);

                // 초기 비밀번호를 아직 바꾸지 않은 토큰은 내 정보 조회·비밀번호 변경만 허용 (403, 3006)
                // 401이 아니라 403인 이유: 프론트엔드가 401을 받으면 로그아웃시키기 때문
                if (jwtTokenProvider.isPasswordChangeRequired(claims) && !isAllowedBeforePasswordChange(request)) {
                    errorResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.PASSWORD_CHANGE_REQUIRED);
                    return;
                }

                // 관리자 API는 토큰의 role 대신 DB의 현재 권한으로 판단한다.
                // (권한을 회수해도 토큰 만료(최대 30분)까지 관리자 API를 쓸 수 있는 문제 방지, 비활성 계정 차단)
                if (request.getRequestURI().startsWith(ADMIN_PATH_PREFIX)) {
                    role = userRepositoryPort.findById(userId)
                            .filter(u -> Boolean.TRUE.equals(u.getActive()))
                            .map(User::getRole)
                            .orElse(null);
                    if (role == null) {
                        request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.INVALID_TOKEN);
                        filterChain.doFilter(request, response); // 인증 없이 진행 → 401
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.EXPIRED_TOKEN);
            } catch (JwtException | IllegalArgumentException e) {
                // 서명 불일치, 형식 오류, subject/role 파싱 실패 등
                log.debug("Invalid JWT: {}", e.getMessage());
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.INVALID_TOKEN);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
