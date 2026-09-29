package com.library.common.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Ответ token endpoint в формате OAuth 2.0 (RFC 6749, §5.1). */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
}
