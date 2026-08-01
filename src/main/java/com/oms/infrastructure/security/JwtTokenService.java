package com.oms.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Infrastructure service responsible for issuing signed JWT access tokens.
 *
 * <p>Tokens carry the username as the {@code sub} claim, the granted authorities as a
 * space-delimited {@code scope} claim (OAuth2 convention), and expire after a
 * configurable time-to-live.</p>
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration timeToLive;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${oms.security.jwt.issuer}") String issuer,
                           @Value("${oms.security.jwt.expiration-minutes}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.timeToLive = Duration.ofMinutes(expirationMinutes);
    }

    /**
     * Signs a new access token for an already-authenticated principal.
     *
     * @param authentication the verified authentication (credentials already checked)
     * @return the compact, URL-safe serialized JWT
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();

        String scope = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(now.plus(timeToLive))
            .subject(authentication.getName())
            .claim("scope", scope)
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Token lifetime in seconds, exposed for the {@code expires_in} response field. */
    public long getTimeToLiveSeconds() {
        return timeToLive.toSeconds();
    }
}
