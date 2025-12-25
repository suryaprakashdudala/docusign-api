package com.docusign.security;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.docusign.entity.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
	@Value("${jwt.secret}")
    private String SECRET_KEY;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder().signWith(getSignKey(), SignatureAlgorithm.HS512)
          .setHeaderParam("typ", "JWT")
          .setIssuer("secure-api")
          .setAudience("secure-app")
          .setSubject(user.getUserName())
          .setExpiration(Date.from(Instant.ofEpochMilli(System.currentTimeMillis()+(1000 * 60 * 60))))
          .claim("userRoles",user.getRoles())
          .claim("userName",user.getUserName())
          .claim("adminAccessEntities", user.getRoles())
          .claim("id",user.getId()).compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String token, String userName) {
        return userName.equals(extractUsername(token)) && !isTokenExpired(token);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }
    

    public String generateTokenForExternalUser(String email, String docToken) {
        return Jwts.builder().signWith(getSignKey(), SignatureAlgorithm.HS512)
          .setHeaderParam("typ", "JWT")
          .setIssuer("secure-api")
          .setAudience("secure-app")
          .setSubject(email)
          .setExpiration(Date.from(Instant.ofEpochMilli(System.currentTimeMillis()+(1000 * 60 * 60))))
          .claim("userRoles",List.of("Viewer"))
          .claim("userName",email)
          .claim("adminAccessEntities", List.of("Viewer"))
          .claim("docToken", docToken)
          .claim("id",email).compact();
    }

    public String extractDocToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("docToken", String.class);
    }
}
