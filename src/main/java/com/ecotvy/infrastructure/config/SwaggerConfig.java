package com.ecotvy.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        // Bearer Token만 허용
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(BEARER_SCHEME);

        // 보안 스키마 정의
        Components components = new Components()
                .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("JWT 토큰을 입력하세요. 예: Bearer {token}"));

        // Info 메타데이터
        Info info = new Info()
                .title("EcoTvy API")
                .description("쓰레기 분류 및 배출 가이드 플랫폼 EcoTvy의 REST API 문서")
                .version("1.0.0");

        return new OpenAPI()
                .info(info)
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}

