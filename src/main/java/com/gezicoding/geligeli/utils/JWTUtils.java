package com.gezicoding.geligeli.utils;

import com.gezicoding.geligeli.constants.JwtConstants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;
import java.time.Duration;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;


@Slf4j
public class JWTUtils {

    private final static Duration JWT_TIME_OUT = Duration.ofMinutes(JwtConstants.JWT_TIME_OUT);

    public static String generateToken(String userId) {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + JWT_TIME_OUT.toMillis()))
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public static Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(JwtConstants.TOKEN_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static Claims parse(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }

        Claims claims = null;
        try {
            claims = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            log.error("解析JWT失败", e);
        }
        return claims;
    }



}
