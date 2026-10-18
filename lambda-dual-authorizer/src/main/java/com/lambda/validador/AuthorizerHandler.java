package com.lambda.validador;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * AWS API Gateway HTTP API (Payload Format v2.0) Lambda Authorizer.
 * Handler: com.lambda.validador.AuthorizerHandler::handleRequest
 * Validates RS256 JWT tokens emitted by either Microsoft Azure Entra ID or AWS Cognito.
 */
public class AuthorizerHandler {

    private static final String DEFAULT_AZURE_ISSUER =
            "https://login.microsoftonline.com/e5372bf0-c5e3-4286-887c-79069f209c1f/v2.0";

    private static final Pattern COGNITO_ISSUER_PATTERN =
            Pattern.compile("^https://cognito-idp\\.[a-z0-9-]+\\.amazonaws\\.com/[a-z0-9-]+_[A-Za-z0-9]+$");

    private final String azureIssuer;
    private final String cognitoIssuer;
    private final ConcurrentMap<String, JWKSource<SecurityContext>> jwkSources = new ConcurrentHashMap<>();

    public AuthorizerHandler() {
        this(
                getenvOrDefault("JWT_ISSUER", DEFAULT_AZURE_ISSUER),
                getenvOrDefault("COGNITO_ISSUER", "")
        );
    }

    public AuthorizerHandler(String azureIssuer, String cognitoIssuer) {
        this.azureIssuer = azureIssuer != null ? azureIssuer.trim() : DEFAULT_AZURE_ISSUER;
        this.cognitoIssuer = cognitoIssuer != null ? cognitoIssuer.trim() : "";
    }

    public void registerJwkSourceForTesting(String issuer, JWKSource<SecurityContext> jwkSource) {
        jwkSources.put(issuer, jwkSource);
    }

    public Map<String, Object> handleRequest(Map<String, Object> event) {
        try {
            String rawToken = extractBearerToken(event);
            if (rawToken == null || rawToken.isBlank()) {
                return unauthorizedResponse("Missing Authorization Bearer token");
            }

            SignedJWT signedJWT = SignedJWT.parse(rawToken);
            JWTClaimsSet unverifiedClaims = signedJWT.getJWTClaimsSet();
            String tokenIssuer = unverifiedClaims.getIssuer();
            if (tokenIssuer == null || tokenIssuer.isBlank()) {
                return unauthorizedResponse("Missing iss claim");
            }

            if (!isAllowedIssuer(tokenIssuer)) {
                return unauthorizedResponse("Untrusted token issuer: " + tokenIssuer);
            }

            JWKSource<SecurityContext> jwkSource = jwkSources.computeIfAbsent(tokenIssuer, this::createRemoteJwkSource);
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
            jwtProcessor.setJWSKeySelector(keySelector);

            JWTClaimsSet verifiedClaims = jwtProcessor.process(signedJWT, null);

            Date now = new Date();
            Date exp = verifiedClaims.getExpirationTime();
            if (exp == null || exp.before(now)) {
                return unauthorizedResponse("Token expired");
            }

            String provider = tokenIssuer.contains("login.microsoftonline.com") ? "AZURE_ENTRA" : "AWS_COGNITO";
            String subject = verifiedClaims.getSubject() != null ? verifiedClaims.getSubject() : "unknown";
            String email = extractEmail(verifiedClaims);
            String role = extractPrimaryRole(verifiedClaims);

            Map<String, Object> context = new HashMap<>();
            context.put("provider", provider);
            context.put("sub", subject);
            context.put("email", email);
            context.put("role", role);
            context.put("issuer", tokenIssuer);

            Map<String, Object> response = new HashMap<>();
            response.put("isAuthorized", true);
            response.put("context", context);
            return response;
        } catch (Exception e) {
            return unauthorizedResponse("Token validation error: " + e.getMessage());
        }
    }

    private boolean isAllowedIssuer(String tokenIssuer) {
        if (tokenIssuer.equalsIgnoreCase(azureIssuer)) {
            return true;
        }
        if (!cognitoIssuer.isBlank() && tokenIssuer.equalsIgnoreCase(cognitoIssuer)) {
            return true;
        }
        return COGNITO_ISSUER_PATTERN.matcher(tokenIssuer).matches();
    }

    private JWKSource<SecurityContext> createRemoteJwkSource(String issuer) {
        try {
            String normalized = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
            String jwksUrl;
            if (normalized.contains("login.microsoftonline.com")) {
                String base = normalized.endsWith("/v2.0")
                        ? normalized.substring(0, normalized.length() - 5)
                        : normalized;
                jwksUrl = base + "/discovery/v2.0/keys";
            } else {
                jwksUrl = normalized + "/.well-known/jwks.json";
            }
            return JWKSourceBuilder.create(URI.create(jwksUrl).toURL()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JWKSource for issuer " + issuer, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractBearerToken(Map<String, Object> event) {
        if (event == null) {
            return null;
        }
        Object headersObj = event.get("headers");
        if (headersObj instanceof Map<?, ?> headers) {
            for (Map.Entry<?, ?> entry : headers.entrySet()) {
                if (entry.getKey() != null && "authorization".equalsIgnoreCase(entry.getKey().toString())) {
                    String val = entry.getValue() != null ? entry.getValue().toString().trim() : "";
                    if (val.regionMatches(true, 0, "Bearer ", 0, 7)) {
                        return val.substring(7).trim();
                    }
                    return val;
                }
            }
        }
        Object identitySourceObj = event.get("identitySource");
        if (identitySourceObj instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
            String val = list.get(0).toString().trim();
            if (val.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return val.substring(7).trim();
            }
            return val;
        }
        return null;
    }

    private String extractEmail(JWTClaimsSet claims) {
        for (String key : List.of("preferred_username", "email", "upn", "cognito:username")) {
            Object val = claims.getClaim(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractPrimaryRole(JWTClaimsSet claims) {
        Object rolesObj = claims.getClaim("roles");
        if (rolesObj instanceof List<?> roles && !roles.isEmpty() && roles.get(0) != null) {
            return roles.get(0).toString();
        }
        Object groupsObj = claims.getClaim("cognito:groups");
        if (groupsObj instanceof List<?> groups && !groups.isEmpty() && groups.get(0) != null) {
            return groups.get(0).toString();
        }
        return "Usuario";
    }

    private Map<String, Object> unauthorizedResponse(String reason) {
        Map<String, Object> response = new HashMap<>();
        response.put("isAuthorized", false);
        response.put("context", Map.of("error", reason));
        return response;
    }

    private static String getenvOrDefault(String name, String defaultValue) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
