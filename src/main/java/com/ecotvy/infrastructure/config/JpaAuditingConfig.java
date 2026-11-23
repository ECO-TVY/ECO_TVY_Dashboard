package com.ecotvy.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity의 @CreatedDate / @LastModifiedDate 자동 기록 활성화.
 * (없으면 CREATE_DT가 null로 INSERT되어 NOT NULL 제약에 걸림)
 * 메인 클래스가 아닌 별도 설정으로 두어 @WebMvcTest 슬라이스 테스트에 영향이 없게 한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
