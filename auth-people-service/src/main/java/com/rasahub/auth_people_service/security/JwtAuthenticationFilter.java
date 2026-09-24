package com.rasahub.auth_people_service.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    JwtAuthenticationFilter.class
            );

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authHeader.substring(7).trim();

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null
                    && jwtService.isTokenValid(token)) {

                String subject =
                        jwtService.extractSubject(token);

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(subject);

                boolean accountOk =
                        userDetails.isEnabled()
                                && userDetails.isAccountNonLocked()
                                && userDetails.isAccountNonExpired()
                                && userDetails.isCredentialsNonExpired();

                if (accountOk) {

                    UsernamePasswordAuthenticationToken authToken =
                            UsernamePasswordAuthenticationToken
                                    .authenticated(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authToken);

                } else {
                    log.debug(
                            "JWT valid but account is not usable"
                    );
                }
            }

        } catch (JwtException
                 | AuthenticationException
                 | IllegalArgumentException e) {

            log.debug(
                    "JWT authentication failed: {}",
                    e.getClass().getSimpleName()
            );
        }

        filterChain.doFilter(request, response);
    }
}