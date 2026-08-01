package com.oms.infrastructure.adapter.input.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Objects (DTOs) for the authentication boundary.
 * These structures isolate the token issuance contract from security internals.
 */
public class AuthDTOs {

    /**
     * Inbound Payload (Token Request). Credentials are validated at the boundary
     * before reaching the authentication provider.
     */
    public record TokenRequest(
            @NotBlank(message = "Username is mandatory")
            String username,

            @NotBlank(message = "Password is mandatory")
            String password
    ) {}

    /**
     * Outbound Payload (Token Response). Follows the OAuth2 token response
     * convention (snake_case fields).
     */
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {}
}
