package com.oms.infrastructure.adapter.input.rest;

import com.oms.infrastructure.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Input Adapter: The Authentication Controller. Exchanges verified user credentials
 * for a signed JWT access token, which clients then present as a Bearer token
 * on all protected endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT access token issuance")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/token")
    @Operation(summary = "Issue an access token", description = "Verifies the provided credentials and returns a signed JWT for Bearer authorization.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials verified; token issued"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    public AuthDTOs.TokenResponse issueToken(@Valid @RequestBody AuthDTOs.TokenRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

        String token = jwtTokenService.generateToken(authentication);
        return new AuthDTOs.TokenResponse(token, "Bearer", jwtTokenService.getTimeToLiveSeconds());
    }
}
