package com.library.auth.web;

import com.library.auth.service.AuthService;
import com.library.auth.web.dto.LoginRequest;
import com.library.auth.web.dto.RegisterRequest;
import com.library.auth.web.dto.UserResponse;
import com.library.common.client.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Аутентификация")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Регистрация читателя (роль READER выдаётся принудительно)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request.username(), request.password(), List.of()));
    }

    @Operation(summary = "Вход: возвращает access token (JWT, RS256)")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @Operation(summary = "Данные текущего токена", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return Map.of(
                "sub", jwt.getSubject(),
                "username", String.valueOf(jwt.getClaimAsString("preferred_username")),
                "roles", roles == null ? List.of() : roles,
                "expiresAt", String.valueOf(jwt.getExpiresAt()));
    }
}
