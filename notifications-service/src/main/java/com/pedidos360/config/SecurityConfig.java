package com.pedidos360.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Pattern COGNITO_ISSUER_PATTERN =
            Pattern.compile("^https://cognito-idp\\.[a-z0-9-]+\\.amazonaws\\.com/[a-z0-9-]+_[A-Za-z0-9]+$");

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200,http://localhost:5173,http://localhost:80,http://localhost,http://*:4200,http://*:80,https://*.amazonaws.com}")
    private String corsAllowedOrigins;

    @Value("${JWT_ISSUER:https://login.microsoftonline.com/e5372bf0-c5e3-4286-887c-79069f209c1f/v2.0}")
    private String azureIssuer;

    @Value("${COGNITO_ISSUER:}")
    private String cognitoIssuer;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Profile("dev")
    public SecurityFilterChain filterChainDev(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain filterChainProd(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/email/enviar", "/actuator/health", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    @Profile("prod")
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder azureDecoder = buildDecoderForIssuer(azureIssuer);
        NimbusJwtDecoder configuredCognitoDecoder = (cognitoIssuer != null && !cognitoIssuer.isBlank())
                ? buildDecoderForIssuer(cognitoIssuer.trim())
                : null;
        ConcurrentMap<String, NimbusJwtDecoder> cognitoDecoders = new ConcurrentHashMap<>();

        return token -> {
            String tokenIssuer = extractIssuerClaim(token);
            if (tokenIssuer != null) {
                if (configuredCognitoDecoder != null && tokenIssuer.equalsIgnoreCase(cognitoIssuer.trim())) {
                    return configuredCognitoDecoder.decode(token);
                }
                if (COGNITO_ISSUER_PATTERN.matcher(tokenIssuer).matches()) {
                    return cognitoDecoders.computeIfAbsent(tokenIssuer, SecurityConfig::buildDecoderForIssuer).decode(token);
                }
            }
            return azureDecoder.decode(token);
        };
    }

    private static NimbusJwtDecoder buildDecoderForIssuer(String issuer) {
        String normalized = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        String jwksUri;
        if (normalized.contains("login.microsoftonline.com")) {
            String base = normalized.endsWith("/v2.0")
                    ? normalized.substring(0, normalized.length() - 5)
                    : normalized;
            jwksUri = base + "/discovery/v2.0/keys";
        } else {
            jwksUri = normalized + "/.well-known/jwks.json";
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static String extractIssuerClaim(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int issIdx = payloadJson.indexOf("\"iss\"");
            if (issIdx < 0) {
                return null;
            }
            int colonIdx = payloadJson.indexOf(':', issIdx);
            int firstQuote = payloadJson.indexOf('"', colonIdx + 1);
            int secondQuote = payloadJson.indexOf('"', firstQuote + 1);
            if (firstQuote > 0 && secondQuote > firstQuote) {
                return payloadJson.substring(firstQuote + 1, secondQuote);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());
        return converter;
    }

    static class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new ArrayList<>(defaultGrantedAuthoritiesConverter.convert(jwt));
            boolean hasRole = false;

            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                for (String role : roles) {
                    if (role != null && !role.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        hasRole = true;
                    }
                }
            }

            List<String> cognitoGroups = jwt.getClaimAsStringList("cognito:groups");
            if (cognitoGroups != null) {
                for (String group : cognitoGroups) {
                    if (group != null && !group.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + group));
                        hasRole = true;
                    }
                }
            }

            if (!hasRole) {
                authorities.add(new SimpleGrantedAuthority("ROLE_Usuario"));
            }

            return authorities;
        }
    }
}

