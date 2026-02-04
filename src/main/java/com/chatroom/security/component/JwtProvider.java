package com.chatroom.security.component;

import com.chatroom.security.entity.SecurityUser;
import com.chatroom.security.exception.InvalidJwtException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final long ttlMillis = 24 * 60 * 60 * 1000L;

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateJwt(SecurityUser securityUser) throws JOSEException {

        List<String> roles = securityUser.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(securityUser.getUsername()) // userPKId
                .claim("roles", roles)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + ttlMillis))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );

        signedJWT.sign(new MACSigner(secretKey));
        return signedJWT.serialize();
    }
    public Authentication getAuthentication(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);   // 解析token
        JWSVerifier verifier = new MACVerifier(secretKey);
        if (!signedJWT.verify(verifier)){
            throw new InvalidJwtException("signature invalid");
        }
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        if (claims.getExpirationTime().before(new Date())){
            throw new InvalidJwtException("token expired");
        }
        String userPKId = claims.getSubject();
        List<String> roles = claims.getStringListClaim("roles");
        SecurityUser securityUser = new SecurityUser(userPKId, null,
                roles.stream()
                        .map(role -> (GrantedAuthority) () -> role)
                        .collect(Collectors.toList()));
        // securityUser->principal（用户身份）；credentials（用户凭证）；authorities（用户权限）
        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }
    public String getUserId(String token) throws ParseException {
        return SignedJWT.parse(token)
                .getJWTClaimsSet()
                .getSubject();
    }

}