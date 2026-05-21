package roomescape.domain.auth.token;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import roomescape.domain.auth.properties.AuthTokenProperties;

@Component
public class RefreshTokenCookieProvider {

    private final AuthTokenProperties authTokenProperties;

    public RefreshTokenCookieProvider(AuthTokenProperties authTokenProperties) {
        this.authTokenProperties = authTokenProperties;
    }

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(authTokenProperties.getRefreshTokenCookieName(), refreshToken)
                .httpOnly(true)
                .secure(authTokenProperties.isRefreshTokenCookieSecure())
                .sameSite(authTokenProperties.getRefreshTokenCookieSameSite())
                .path(authTokenProperties.getRefreshTokenCookiePath())
                .maxAge(Duration.ofSeconds(authTokenProperties.getRefreshTokenExpiration()))
                .build();
    }

    public ResponseCookie expire() {
        return ResponseCookie.from(authTokenProperties.getRefreshTokenCookieName(), "")
                .httpOnly(true)
                .secure(authTokenProperties.isRefreshTokenCookieSecure())
                .sameSite(authTokenProperties.getRefreshTokenCookieSameSite())
                .path(authTokenProperties.getRefreshTokenCookiePath())
                .maxAge(0)
                .build();
    }
}
