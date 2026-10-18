package com.lambda.validador;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizerHandlerTest {

    private static final String AZURE_ISSUER = "https://login.microsoftonline.com/e5372bf0-c5e3-4286-887c-79069f209c1f/v2.0";
    private static final String COGNITO_ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AbCdEf123";

    private RSAKey rsaKey;
    private AuthorizerHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-kid-1").generate();
        handler = new AuthorizerHandler(AZURE_ISSUER, COGNITO_ISSUER);
        handler.registerJwkSourceForTesting(AZURE_ISSUER, new ImmutableJWKSet<>(new JWKSet(rsaKey.toPublicJWK())));
        handler.registerJwkSourceForTesting(COGNITO_ISSUER, new ImmutableJWKSet<>(new JWKSet(rsaKey.toPublicJWK())));
    }

    @Test
    void shouldAuthorizeValidAzureEntraToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(AZURE_ISSUER)
                .subject("azure-user-123")
                .claim("preferred_username", "admin@pedidos360.onmicrosoft.com")
                .claim("roles", List.of("Administrador"))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();

        String token = signToken(claims);
        Map<String, Object> result = handler.handleRequest(Map.of(
                "headers", Map.of("Authorization", "Bearer " + token)
        ));

        assertEquals(true, result.get("isAuthorized"));
        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) result.get("context");
        assertEquals("AZURE_ENTRA", ctx.get("provider"));
        assertEquals("Administrador", ctx.get("role"));
    }

    @Test
    void shouldAuthorizeValidAwsCognitoToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(COGNITO_ISSUER)
                .subject("cognito-user-456")
                .claim("email", "cliente@empresa.cl")
                .claim("cognito:groups", List.of("Despachador"))
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();

        String token = signToken(claims);
        Map<String, Object> result = handler.handleRequest(Map.of(
                "headers", Map.of("authorization", "Bearer " + token)
        ));

        assertEquals(true, result.get("isAuthorized"));
        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = (Map<String, Object>) result.get("context");
        assertEquals("AWS_COGNITO", ctx.get("provider"));
        assertEquals("Despachador", ctx.get("role"));
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(AZURE_ISSUER)
                .subject("azure-user-123")
                .expirationTime(Date.from(Instant.now().minusSeconds(60)))
                .build();

        String token = signToken(claims);
        Map<String, Object> result = handler.handleRequest(Map.of(
                "headers", Map.of("Authorization", "Bearer " + token)
        ));

        assertEquals(false, result.get("isAuthorized"));
    }

    @Test
    void shouldRejectUntrustedIssuer() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("https://evil.example.com/v2.0")
                .subject("hacker")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();

        String token = signToken(claims);
        Map<String, Object> result = handler.handleRequest(Map.of(
                "headers", Map.of("Authorization", "Bearer " + token)
        ));

        assertEquals(false, result.get("isAuthorized"));
    }

    private String signToken(JWTClaimsSet claims) throws Exception {
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claims
        );
        signedJWT.sign(new RSASSASigner(rsaKey));
        return signedJWT.serialize();
    }
}
