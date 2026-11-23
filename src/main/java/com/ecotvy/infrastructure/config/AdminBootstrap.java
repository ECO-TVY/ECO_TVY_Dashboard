package com.ecotvy.infrastructure.config;

import com.ecotvy.application.port.out.UserRepositoryPort;
import com.ecotvy.domain.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 첫 관리자 지정.
 * 관리자 API는 관리자만 호출할 수 있으므로 최초 1명은 여기서 만든다.
 *
 * admin.bootstrap.email(환경변수 ADMIN_BOOTSTRAP_EMAIL)에 이미 가입한 계정의 이메일을 넣으면,
 * 서버 시작 시 관리자가 한 명도 없을 때에만 그 계정을 ADMIN으로 올린다.
 * - 비밀번호를 설정 파일에 두지 않는다 (먼저 일반 회원가입으로 계정을 만든다)
 * - 관리자가 이미 있으면 아무것도 하지 않으므로 값을 남겨 둬도 이후 권한 상승 통로가 되지 않는다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepositoryPort userRepositoryPort;

    @Value("${admin.bootstrap.email:}")
    private String bootstrapEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(bootstrapEmail)) {
            return;
        }
        if (userRepositoryPort.countByRole(UserRole.ADMIN) > 0) {
            log.info("admin.bootstrap.email is set but an admin already exists; nothing to do (you can remove the setting)");
            return;
        }
        userRepositoryPort.findByEmail(bootstrapEmail.trim()).ifPresentOrElse(user -> {
            user.changeRole(UserRole.ADMIN);
            userRepositoryPort.save(user);
            log.warn("No admin existed; promoted {} (id={}) to ADMIN via admin.bootstrap.email", user.getEmail(), user.getId());
        }, () -> log.warn("admin.bootstrap.email={} is not a registered account. Sign up with that email first, then restart.",
                bootstrapEmail.trim()));
    }
}
