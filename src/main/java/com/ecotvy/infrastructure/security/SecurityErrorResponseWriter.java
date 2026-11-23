package com.ecotvy.infrastructure.security;

import com.ecotvy.infrastructure.dto.response.ApiResponse;
import com.ecotvy.infrastructure.error.enums.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 필터 단계에서 발생한 401/403을 GlobalExceptionHandler와 같은 ApiResponse 형식의 JSON으로 응답한다.
 * (필터 예외는 @RestControllerAdvice까지 도달하지 않으므로 별도 처리 필요)
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** 인증 정보가 없거나 토큰이 유효하지 않음 → 401 */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attr = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode = attr instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED;
        write(response, HttpServletResponse.SC_UNAUTHORIZED, errorCode);
    }

    /** 인증은 되었지만 권한 부족 (예: USER가 /api/admin/** 접근) → 403 */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN);
    }

    /** 필터에서 직접 JSON 오류를 응답할 때도 사용 (예: 비밀번호 변경 필요) */
    public void write(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(errorCode.getMessage(), String.valueOf(errorCode.getCode())));
    }
}
