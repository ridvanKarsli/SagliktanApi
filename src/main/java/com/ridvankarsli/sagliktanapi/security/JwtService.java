package com.ridvankarsli.sagliktanapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// JWT üretimi ve doğrulaması. Access token ve refresh token için aynı
// mekanizma kullanılıyor, sadece geçerlilik süresi (expiration) farklı.
@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // "Aktif Oturumlar" (görev #305): sid claim'i bir refresh token'ı DB'deki
    // RefreshSession satırına bağlar - ham token asla saklanmıyor, sadece bu
    // rastgele kimlik. Access token'a da AYNI sid ekleniyor ki bir API isteği
    // sırasında "bu istek hangi oturumdan geliyor" bilinsin (bkz.
    // JwtAuthenticationFilter'ın request'e sid'i attribute olarak koyması) -
    // bu da kullanıcıya "şu an kullandığın oturum" bilgisini göstermeyi
    // (current: true) mümkün kılıyor.
    public String generateAccessToken(UserDetails userDetails, String sessionId) {
        return buildToken(Map.of("type", "access", "sid", sessionId), userDetails, accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails, String sessionId) {
        return buildToken(Map.of("type", "refresh", "sid", sessionId), userDetails, refreshTokenExpirationMs);
    }

    public String extractSessionId(String token) {
        return extractClaim(token, claims -> claims.get("sid", String.class));
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList());

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // "access" ya da "refresh" - token tipi karışıklığını önlemek için
    // (bkz. JwtAuthenticationFilter ve AuthServiceImpl.refresh() kullanımı).
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
