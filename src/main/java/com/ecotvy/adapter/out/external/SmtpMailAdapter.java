package com.ecotvy.adapter.out.external;

import com.ecotvy.application.port.out.MailPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * spring.mail.* (환경변수 MAIL_*) 설정의 SMTP 서버로 발송.
 * MAIL_HOST가 비어 있으면 메일 기능이 꺼진 상태로 동작한다 — 서버 기동은 막지 않고, 발송 시 예외를 던진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpMailAdapter implements MailPort {

    // 메일 설정이 없으면 Spring이 JavaMailSender를 만들지 않을 수 있으므로 필수 의존성으로 받지 않는다
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${app.mail.from:no-reply@ecotvy.local}")
    private String from;

    @PostConstruct
    void logStatus() {
        if (!isConfigured()) {
            log.warn("Mail is not configured (MAIL_HOST is empty). Password reset mails will not be sent; "
                    + "admins can still reset passwords from the admin page.");
        } else {
            log.info("Mail configured: host={}, from={}", host, from);
        }
    }

    @Override
    public void send(String to, String subject, String body) {
        JavaMailSender mailSender = isConfigured() ? mailSenderProvider.getIfAvailable() : null;
        if (mailSender == null) {
            throw new IllegalStateException("Mail is not configured (set MAIL_HOST and related MAIL_* variables)");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private boolean isConfigured() {
        return StringUtils.hasText(host);
    }
}
