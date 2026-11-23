package com.ecotvy.application.service;

import com.ecotvy.application.port.in.PasswordResetUseCase;
import com.ecotvy.application.port.out.CachePort;
import com.ecotvy.application.port.out.MailPort;
import com.ecotvy.application.port.out.PasswordResetTokenRepositoryPort;
import com.ecotvy.application.port.out.UserRepositoryPort;
import com.ecotvy.domain.user.PasswordResetToken;
import com.ecotvy.domain.user.User;
import com.ecotvy.infrastructure.error.enums.ErrorCode;
import com.ecotvy.infrastructure.exception.BusinessException;
import com.ecotvy.infrastructure.exception.ResourceNotFoundException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService implements PasswordResetUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** 임시 비밀번호용 문자 (헷갈리는 0 O 1 l I 제외) */
    private static final String LETTERS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordResetTokenRepositoryPort tokenRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final MailPort mailPort;
    private final CachePort cachePort;

    /** 메일은 요청 스레드와 분리해서 보낸다 (응답 시간 차이로 가입 여부가 드러나지 않게) */
    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(2);

    @Value("${app.password-reset.ttl:30m}")
    private Duration tokenTtl;

    @Value("${app.password-reset.cooldown:60s}")
    private Duration cooldown;

    /** 메일 링크가 가리킬 프론트엔드 주소 */
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @PreDestroy
    void shutdown() {
        mailExecutor.shutdown();
    }

    @Override
    public void requestReset(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
        String email = rawEmail.trim();

        // 같은 이메일로 짧은 시간에 여러 번 요청하면 메일을 다시 보내지 않는다 (메일 폭탄 방지)
        String cooldownKey = "pwreset:cooldown:" + email.toLowerCase(Locale.ROOT);
        if (cachePort.get(cooldownKey, String.class) != null) {
            log.info("Password reset request ignored (cooldown)");
            return;
        }
        cachePort.put(cooldownKey, "1", cooldown.toSeconds());

        User user = userRepositoryPort.findByEmail(email).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || user.getPassword() == null) {
            // 가입되지 않은 이메일 등: 응답은 동일하게, 아무것도 보내지 않음
            log.info("Password reset requested for an address without a usable account");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        tokenRepositoryPort.invalidateUnusedForUser(user.getId(), now); // 이전 링크는 무효
        String rawToken = newToken();
        tokenRepositoryPort.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .expiresAt(now.plus(tokenTtl))
                .build());

        String link = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;
        String body = """
                %s님, 안녕하세요.

                ECO_TVY 비밀번호 재설정을 요청하셨습니다.
                아래 링크에서 새 비밀번호를 설정해 주세요. 링크는 %d분 동안 한 번만 사용할 수 있습니다.

                %s

                직접 요청하지 않으셨다면 이 메일을 무시하세요. 비밀번호는 바뀌지 않습니다.
                """.formatted(user.getNickname(), tokenTtl.toMinutes(), link);
        String to = user.getEmail();

        // 커밋 후에만 발송 (토큰 저장이 롤백됐는데 링크가 나가는 일 방지), 실패해도 응답은 같게
        runAfterCommit(() -> mailExecutor.execute(() -> {
            try {
                mailPort.send(to, "[ECO_TVY] 비밀번호 재설정 안내", body);
                log.info("Password reset mail sent to user id={}", user.getId());
            } catch (Exception e) {
                log.error("Failed to send password reset mail to user id={}", user.getId(), e);
            }
        }));
    }

    @Override
    public void confirmReset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = tokenRepositoryPort.findByTokenHash(sha256(rawToken.trim()))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_INVALID));
        User user = token.getUser();
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_INVALID);
        }
        AccountRules.password(newPassword);

        user.changePassword(passwordEncoder.encode(newPassword)); // '변경 필요' 상태도 해제
        userRepositoryPort.save(user);
        token.markUsed(now);
        tokenRepositoryPort.save(token);
        tokenRepositoryPort.invalidateUnusedForUser(user.getId(), now);
        log.info("Password reset completed via mail link for user id={}", user.getId());
    }

    @Override
    public String resetByAdmin(Long actorId, Long targetUserId) {
        if (targetUserId.equals(actorId)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "자신의 비밀번호는 '비밀번호 변경'에서 바꿔 주세요");
        }
        User user = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String tempPassword = newTempPassword();
        user.changePassword(passwordEncoder.encode(tempPassword));
        user.requirePasswordChange(); // 임시 비밀번호로 로그인하면 바로 변경하게
        userRepositoryPort.save(user);
        tokenRepositoryPort.invalidateUnusedForUser(user.getId(), LocalDateTime.now()); // 이미 보낸 메일 링크도 무효
        log.info("Password reset by admin id={} for user id={}", actorId, targetUserId);
        return tempPassword;
    }

    private static void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /** 32바이트 난수 → URL에 그대로 넣을 수 있는 Base64url (43자) */
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 12자, 영문·숫자 각각 1자 이상 (회원가입 비밀번호 규칙 충족) */
    private static String newTempPassword() {
        String all = LETTERS + DIGITS;
        char[] chars = new char[12];
        chars[0] = LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        chars[1] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int i = 2; i < chars.length; i++) {
            chars[i] = all.charAt(RANDOM.nextInt(all.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) { // 섞기
            int j = RANDOM.nextInt(i + 1);
            char t = chars[i];
            chars[i] = chars[j];
            chars[j] = t;
        }
        return new String(chars);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
