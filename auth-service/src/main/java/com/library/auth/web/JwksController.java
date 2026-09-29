package com.library.auth.web;

import com.library.auth.service.JwtIssuer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Публичные ключи для проверки подписи. */
@Tag(name = "OAuth 2.0")
@RestController
public class JwksController {

    private final JwtIssuer jwtIssuer;

    public JwksController(JwtIssuer jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    @Operation(summary = "JWKS: публичные ключи для проверки JWT")
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtIssuer.publicJwks().toJSONObject();
    }
}
