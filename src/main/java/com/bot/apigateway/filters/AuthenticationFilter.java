package com.bot.apigateway.filters;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    public static class Config { }

    public AuthenticationFilter() {
        super(Config.class);
    }
    @Autowired
    private RouteValidator routeValidator;
    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Override
    public GatewayFilter apply(Config config) {
        return (((exchange, chain) -> {
            LOGGER.info("[API REQUEST]: " + exchange.getRequest().getURI().getPath());
            if (HttpMethod.OPTIONS.matches(String.valueOf(exchange.getRequest().getMethod()))) {
                return chain.filter(exchange); // Skip authentication for OPTIONS requests
            }

            if (exchange.getRequest().getURI().getPath().startsWith("/resources/")) {
                return chain.filter(exchange);
            }

            ServerWebExchange modifiedExchange = exchange;
            if (routeValidator.isSecured.test(exchange.getRequest())) {
                // check Authorization header -- ServerWebExchange
                LOGGER.info("[URL REQUESTED]: " + exchange.getRequest().getURI().getPath());
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new RuntimeException("Unauthorization access. Token is missing.");
                }

                String authorizationHeader = Objects.requireNonNull(exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION)).get(0);
                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer")) {
                    authorizationHeader = authorizationHeader.substring(7);
                }

                try {
                    String secret = "bottomhalfx12#@-emstum-api-servic-x#20$46@3211";
                    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
                    SecretKey key = Keys.hmacShaKeyFor(keyBytes);

                Claims claims = Jwts.parser()
                        .setSigningKey(key)
                        .parseClaimsJws(authorizationHeader)
                        .getBody();

                String sid = claims.get("sid", String.class);
                String user = claims.get("JBot", String.class);
                String companyCode = claims.get("CompanyCode", String.class);

                modifiedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .headers(httpHeaders -> httpHeaders.add("userDetail", user))
                                .headers(httpHeaders -> httpHeaders.add("sid", sid))
                                .headers(httpHeaders -> httpHeaders.add("companyCode", companyCode))
                                .build())
                        .build();

                } catch (ExpiredJwtException e) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session got expired");
                } catch (Exception ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unauthorized access. Please try with valid token.");
                }
            }

            return chain.filter(modifiedExchange);
        }));
    }
}
