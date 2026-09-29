package com.library.auth.web;

import com.library.auth.service.AuthService;
import com.library.common.client.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Token endpoint для вызовов «сервис → сервис» (RFC 6749, grant_type=client_credentials). */
@Tag(name = "OAuth 2.0")
@RestController
public class OAuth2TokenController {

    private final AuthService authService;

    public OAuth2TokenController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Токен для сервиса (client_credentials, scope=internal)")
    @PostMapping(path = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse token(@RequestParam("grant_type") String grantType,
                               @RequestParam("client_id") String clientId,
                               @RequestParam("client_secret") String clientSecret) {
        return authService.issueServiceToken(grantType, clientId, clientSecret);
    }
}
