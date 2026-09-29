package com.library.auth;

import com.library.auth.domain.UserAccount;
import com.library.auth.service.AuthService;
import com.library.common.api.ApiException;
import com.library.common.client.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Токен подписан приватным ключом и принимается публичным. */
@SpringBootTest
class JwtIssuingTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void issuedUserTokenIsVerifiableByPublicKeyAndCarriesRoles() {
        UserAccount user = authService.register("reader-test", "very-secret-pass", List.of());

        TokenResponse response = authService.login("reader-test", "very-secret-pass");
        Jwt jwt = jwtDecoder.decode(response.accessToken());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("READER");
        assertThat(jwt.getExpiresAt()).isNotNull();
    }

    @Test
    void serviceTokenRequiresValidClientSecret() {
        TokenResponse token = authService.issueServiceToken("client_credentials", "internal-service", "internal-secret");
        assertThat(jwtDecoder.decode(token.accessToken()).getClaimAsString("scope")).isEqualTo("internal");

        assertThatThrownBy(() -> authService.issueServiceToken("client_credentials", "internal-service", "wrong"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Неверные данные клиента");
    }

    @Test
    void wrongPasswordIsRejected() {
        authService.register("reader-2", "very-secret-pass", List.of());

        assertThatThrownBy(() -> authService.login("reader-2", "not-the-password"))
                .isInstanceOf(ApiException.class);
    }
}
