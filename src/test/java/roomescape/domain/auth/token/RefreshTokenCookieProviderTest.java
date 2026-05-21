package roomescape.domain.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import roomescape.domain.auth.properties.AuthTokenProperties;

class RefreshTokenCookieProviderTest {

    private RefreshTokenCookieProvider cookieProvider;
    private AuthTokenProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuthTokenProperties();
        properties.setRefreshTokenCookieName("refreshToken");
        properties.setRefreshTokenCookiePath("/auth");
        properties.setRefreshTokenCookieSecure(true);
        properties.setRefreshTokenCookieSameSite("Strict");
        properties.setRefreshTokenExpiration(604800); // 7 days

        cookieProvider = new RefreshTokenCookieProvider(properties);
    }

    @Test
    @DisplayName("create 호출 시 지정된 값과 properties 설정이 적용된 리프레시 토큰용 ResponseCookie를 반환한다")
    void create_cookie_success() {
        // when
        ResponseCookie cookie = cookieProvider.create("token_abc_123");

        // then
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("token_abc_123");
        assertThat(cookie.getPath()).isEqualTo("/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(604800L);
    }

    @Test
    @DisplayName("expire 호출 시 maxAge가 0이고 빈 값을 가진 만료용 ResponseCookie를 반환한다")
    void expire_cookie_success() {
        // when
        ResponseCookie cookie = cookieProvider.expire();

        // then
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}
