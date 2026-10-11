package com.rasahub.product_inventory_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(
            @Value("${jwt.secret}") String secret
    ) {
        this.signingKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes()
                );
    }

    public Long extractAccountId(String token) {

        String subject =
                extractSubject(token);

        return Long.valueOf(subject);
    }

    public String extractSubject(String token) {

        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public String extractRole(String token) {

        return extractClaim(
                token,
                claims -> claims.get(
                        "role",
                        String.class
                )
        );
    }

    public boolean isTokenValid(String token) {

        try {
            return !isTokenExpired(token);

        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {

        return extractClaim(
                token,
                Claims::getExpiration
        ).before(new Date());
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        Claims claims =
                Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        return resolver.apply(claims);
    }
}