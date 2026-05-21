package roomescape.domain.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.auth.properties.AuthTokenProperties;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE = "ACCESS";

    private final SecretKey secretKey;
    private final String jwtIssuer;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final String authorizationScheme;
    private final String hashAlgorithm;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public JwtTokenProvider(
            AuthTokenProperties authTokenProperties,
            Clock clock
    ) {
        this.jwtIssuer = authTokenProperties.getIssuer();
        this.accessTokenExpiration = authTokenProperties.getAccessTokenExpiration();
        this.refreshTokenExpiration = authTokenProperties.getRefreshTokenExpiration();
        this.authorizationScheme = authTokenProperties.getType();
        this.hashAlgorithm = authTokenProperties.getHashAlgorithm();
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(authTokenProperties.getSecretKey()));
        this.secureRandom = new SecureRandom();
    }

    public JwtTokenPair createToken(Long userId, String username, String role) {
        Instant now = clock.instant();

        String accessToken = createAccessToken(userId, username, role, now);
        String refreshToken = createRefreshToken();

        return new JwtTokenPair(
                accessToken,
                refreshToken,
                authorizationScheme,
                accessTokenExpiration,
                refreshTokenExpiration
        );
    }

    public String createAccessToken(Long userId, String username, String role, Instant now) {
        validateAccessTokenCreationInput(userId, username, role, now);
        Instant expiresAt = now.plusSeconds(accessTokenExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuer(jwtIssuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .setId(UUID.randomUUID().toString())
                .claim("type", TOKEN_TYPE)
                .claim("username", username)
                .claim("role", role)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public void validateAccessToken(String accessToken) {
        getClaims(accessToken);
    }

    public String getPayload(String accessToken) {
        Claims claims = getClaims(accessToken);
        return claims.getSubject();
    }

    public Long getUserId(String accessToken) {
        try {
            return Long.valueOf(getPayload(accessToken));
        } catch (NumberFormatException exception) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    public Claims getClaims(String accessToken) {
        Claims claims = parseClaims(accessToken);
        validateTokenType(claims);
        return claims;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .requireIssuer(jwtIssuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateTokenType(Claims claims) {
        String type = claims.get("type", String.class);
        if (!TOKEN_TYPE.equals(type)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateAccessTokenCreationInput(Long userId, String username, String role, Instant now) {
        if (userId == null || username == null || username.isBlank() || role == null || role.isBlank() || now == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Refresh Token 해시에 실패했습니다.", exception);
        }
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
