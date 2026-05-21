package roomescape.domain.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.auth.properties.AuthTokenProperties;

class JwtTokenProviderTest {

    private static final String BASE64_SECRET = "dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1nZW5lcmF0aW9uLXdpdGgtbWluaW11bS0yNTYtYml0cw==";

    private JwtTokenProvider jwtTokenProvider;
    private Clock clock;

    @BeforeEach
    void setUp() {
        AuthTokenProperties properties = new AuthTokenProperties();
        properties.setSecretKey(BASE64_SECRET);
        properties.setIssuer("test-issuer");
        properties.setAccessTokenExpiration(3600); // 1 hour
        properties.setRefreshTokenExpiration(604800); // 7 days
        properties.setType("Bearer");
        properties.setHashAlgorithm("SHA-256");

        clock = Clock.fixed(Instant.parse("2026-05-21T18:00:00Z"), ZoneId.of("UTC"));
        jwtTokenProvider = new JwtTokenProvider(properties, clock);
    }

    @Test
    @DisplayName("createToken 호출 시 올바른 만료 시간과 속성을 가진 AccessToken과 RefreshToken 쌍을 반환한다")
    void createToken_success() {
        // when
        JwtTokenPair tokenPair = jwtTokenProvider.createToken(1L, "user1", "USER");

        // then
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
        assertThat(tokenPair.type()).isEqualTo("Bearer");
        assertThat(tokenPair.accessTokenExpiresIn()).isEqualTo(3600);
        assertThat(tokenPair.refreshTokenExpiresIn()).isEqualTo(604800);
    }

    @Test
    @DisplayName("올바른 AccessToken에서 사용자 ID를 성공적으로 파싱하여 가져온다")
    void getUserId_success() {
        // given
        JwtTokenPair tokenPair = jwtTokenProvider.createToken(42L, "user1", "USER");

        // when
        Long userId = jwtTokenProvider.getUserId(tokenPair.accessToken());

        // then
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("조작되었거나 서명이 올바르지 않은 토큰 검증 시 INVALID_TOKEN 예외를 반환한다")
    void getUserId_invalid_token_throws() {
        // given
        String manipulatedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MiIsInR5cGUiOiJBQ0NFU1MiLCJ1c2VybmFtZSI6InVzZXIxIiwicm9sZSI6IlVTRVIifQ.invalid_signature_value";

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.getUserId(manipulatedToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_TOKEN);
                });
    }

    @Test
    @DisplayName("입력받은 문자열에 대해 지정된 해시 알고리즘(SHA-256)으로 Hex 해시값을 생성한다")
    void hash_success() {
        // given
        String token = "my-refresh-token-value";

        // when
        String hashed1 = jwtTokenProvider.hash(token);
        String hashed2 = jwtTokenProvider.hash(token);

        // then
        assertThat(hashed1).isEqualTo(hashed2);
        assertThat(hashed1).hasSize(64); // SHA-256 hex string length is 64 characters
    }
}
