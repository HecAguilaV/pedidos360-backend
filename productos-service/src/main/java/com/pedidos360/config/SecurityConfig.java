package com.pedidos360.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200,http://localhost:5173,http://localhost:80,http://localhost,http://*:4200,http://*:80,https://*.amazonaws.com}")
    private String corsAllowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
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
                .requestMatchers("/actuator/health", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/productos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/productos/**").hasAnyRole("Administrador", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/productos/**").hasAnyRole("Administrador", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/productos/**").hasAnyRole("Administrador", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CustomJwtGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * Extracts delegated 'scp'/'scope' claims (SCOPE_...),
     * Entra app roles 'roles' claims (ROLE_...), and Cognito 'cognito:groups'.
     */
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
                        if ("Administrador".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_Administrador"));
                            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }
                        hasRole = true;
                    }
                }
            }

            List<String> cognitoGroups = jwt.getClaimAsStringList("cognito:groups");
            if (cognitoGroups != null) {
                for (String group : cognitoGroups) {
                    if (group != null && !group.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + group));
                        if ("Administrador".equalsIgnoreCase(group) || "ADMIN".equalsIgnoreCase(group)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_Administrador"));
                            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        }
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

