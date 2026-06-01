package com.company.admin.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void rejectsTemplateJwtSecret() {
        assertThatThrownBy(() -> new JwtService("change_this_jwt_secret_before_deploy", 120))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be replaced");
    }

    @Test
    void generatesAndParsesTokenWithStrongSecret() {
        JwtService jwtService = new JwtService("strong-production-like-jwt-secret-32-bytes", 120);

        String token = jwtService.generateToken("superadmin");

        assertThat(jwtService.parseUsername(token)).contains("superadmin");
    }
}
