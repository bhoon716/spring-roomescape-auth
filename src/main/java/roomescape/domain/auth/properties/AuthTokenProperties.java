package roomescape.domain.auth.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.auth.token")
public class AuthTokenProperties {

    @NotBlank
    @Size(min = 44)
    private String secretKey;

    @NotBlank
    private String issuer;

    @Positive
    private long accessTokenExpiration;

    @Positive
    private long refreshTokenExpiration;

    @NotBlank
    private String refreshTokenCookieName;

    @NotBlank
    private String refreshTokenCookiePath;

    private boolean refreshTokenCookieSecure;

    @NotBlank
    @Pattern(regexp = "Strict|Lax|None")
    private String refreshTokenCookieSameSite;

    @NotBlank
    private String type;

    @NotBlank
    @Pattern(regexp = "SHA-256|SHA-384|SHA-512")
    private String hashAlgorithm;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getRefreshTokenCookieName() {
        return refreshTokenCookieName;
    }

    public void setRefreshTokenCookieName(String refreshTokenCookieName) {
        this.refreshTokenCookieName = refreshTokenCookieName;
    }

    public String getRefreshTokenCookiePath() {
        return refreshTokenCookiePath;
    }

    public void setRefreshTokenCookiePath(String refreshTokenCookiePath) {
        this.refreshTokenCookiePath = refreshTokenCookiePath;
    }

    public boolean isRefreshTokenCookieSecure() {
        return refreshTokenCookieSecure;
    }

    public void setRefreshTokenCookieSecure(boolean refreshTokenCookieSecure) {
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
    }

    public String getRefreshTokenCookieSameSite() {
        return refreshTokenCookieSameSite;
    }

    public void setRefreshTokenCookieSameSite(String refreshTokenCookieSameSite) {
        this.refreshTokenCookieSameSite = refreshTokenCookieSameSite;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    @AssertTrue(message = "SameSite=None requires Secure=true")
    public boolean isSameSiteNoneSecure() {
        return !"None".equals(refreshTokenCookieSameSite) || refreshTokenCookieSecure;
    }
}
