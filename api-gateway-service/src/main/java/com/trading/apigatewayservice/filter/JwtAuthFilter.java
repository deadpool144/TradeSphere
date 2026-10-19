package com.trading.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;

@Component
@Slf4j
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    // FIXED: Formatted signature argument to map our internal Config bounds cleanly
    public GatewayFilter apply(JwtAuthFilter.Config config) {
        return (exchange, chain) ->  {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // 1. Log the exact incoming URL and Path segments
            String absoluteUri = request.getURI().toString();
            String relativePath = request.getURI().getPath();
            String httpMethod = request.getMethod().name();

            log.info("============== INTERCEPTING GATEWAY REQUEST ==============");
            log.info("HTTP Method:  {}", httpMethod);
            log.info("Absolute URI: {}", absoluteUri);
            log.info("Clean Path:   {}", relativePath);
            log.info("==========================================================");


            // Skip JWT validation filters for public endpoints
            if (isPublicEndpoints(path)) {
                log.info("Public endpoint hit - skipping JWT filter for path: {}", path);
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst("Authorization");

            // FIXED: Combined string extraction check evaluates cleanly as a boolean condition
            if (authHeader == null || authHeader.isBlank()) {
                log.debug("Authorization header is missing: {}", path);
                return unauthorizedResponse(exchange, "Authorization headers missing");
            }

            if (!authHeader.startsWith("Bearer ")) {
                return unauthorizedResponse(exchange, "Invalid Authorization format");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = extractClaims(token);
                String userId = claims.get("userId", String.class);
                String email = claims.getSubject();

                log.debug("userId : {} email : {}", userId, email);

                ServerHttpRequest modifiedRequest = request
                        .mutate()
                        .header("X-User-Id", userId)
                        .build();

                return chain.filter(
                        exchange.mutate()
                                .request(modifiedRequest)
                                .build());

            } catch (Exception e) {
                log.warn("Invalid JWT Token : {}", e.getMessage());
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }
        }; // FIXED: Restored uniform return lambda statement enclosures cleanly
    }

    private Claims extractClaims(String token) {
        // FIXED: Upgraded parser fluent chains to adhere to modern standard JJWT builder blocks
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        log.warn("unauthorized :{}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublicEndpoints(String path) {
        return path.contains("/register") ||
                path.contains("/login") ||
                path.contains("health") ||
                path.contains("ws");
    }

    public static class Config {
        // Configuration fields go here if needed
    }
}
