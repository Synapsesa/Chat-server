package com.synapse.chat_service.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenProvider(@Value("${secret.key}") String secretKey) {
        this.algorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(this.algorithm).build();
    }

    public final Authentication verifyAndDecode(String token) throws JWTVerificationException {
        DecodedJWT decodedJWT = verifier.verify(token);
        String userId = decodedJWT.getSubject();

        Claim authClaim = decodedJWT.getClaim("role");
        String[] roles = authClaim.asString().split(",");

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(roles)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        
        UserDetails principal = new User(userId, "", authorities);
        
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }
}
