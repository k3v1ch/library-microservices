package com.library.auth.service;

import com.library.auth.config.AuthProperties;
import com.library.auth.domain.UserAccount;
import com.library.common.client.TokenResponse;
import com.library.common.security.SecurityDefaults;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Выпуск и подпись JWT алгоритмом RS256. */
@Component
public class JwtIssuer {

    private final RSAKey key;
    private final JWSSigner signer;
    private final AuthProperties properties;

    public JwtIssuer(RSAKey key, AuthProperties properties) throws JOSEException {
        this.key = key;
        this.signer = new RSASSASigner(key);
        this.properties = properties;
    }

    /** Токен пользователя: субъект — id учётной записи, роли в claim {@code roles}. */
    public TokenResponse issueUserToken(UserAccount user) {
        return sign(new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim("preferred_username", user.getUsername())
                .claim("roles", user.getRoles()), properties.getTokenTtl());
    }

    /** Токен сервиса (client_credentials): даёт доступ только к /internal/** через scope. */
    public TokenResponse issueServiceToken(String clientId) {
        return sign(new JWTClaimsSet.Builder()
                .subject(clientId)
                .claim("scope", SecurityDefaults.SCOPE_INTERNAL), properties.getServiceTokenTtl());
    }

    public JWKSet publicJwks() {
        return new JWKSet(key.toPublicJWK());
    }

    private TokenResponse sign(JWTClaimsSet.Builder claims, Duration ttl) {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = claims
                .issuer(properties.getIssuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claimsSet);
        try {
            jwt.sign(signer);
        } catch (JOSEException ex) {
            throw new IllegalStateException("Не удалось подписать токен", ex);
        }
        return new TokenResponse(jwt.serialize(), "Bearer", ttl.toSeconds());
    }
}
