package roomescape.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    @Test
    @DisplayName("create 호출 시 isRevoked가 false인 RefreshToken 객체를 생성한다")
    void create_success() {
        // given
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // when
        RefreshToken token = RefreshToken.create(1L, "hash_value", expiresAt);

        // then
        assertThat(token.getId()).isNull();
        assertThat(token.getUserId()).isEqualTo(1L);
        assertThat(token.getTokenHash()).isEqualTo("hash_value");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("만료 시점보다 현재 시간이 이전이면 isExpired는 false를 반환하고, 같거나 이후이면 true를 반환한다")
    void isExpired_logic() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 5, 21, 18, 0, 0);
        RefreshToken token = RefreshToken.create(1L, "hash_value", expiresAt);

        // when & then
        assertThat(token.isExpired(expiresAt.minusSeconds(1))).isFalse();
        assertThat(token.isExpired(expiresAt)).isTrue();
        assertThat(token.isExpired(expiresAt.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("revoke 호출 시 기존의 필드 정보는 동일하고 isRevoked만 true인 새 RefreshToken 객체를 반환한다")
    void revoke_success() {
        // given
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        RefreshToken token = RefreshToken.of(10L, 1L, "hash_value", expiresAt, false);

        // when
        RefreshToken revokedToken = token.revoke();

        // then
        assertThat(revokedToken).isNotSameAs(token);
        assertThat(revokedToken.getId()).isEqualTo(10L);
        assertThat(revokedToken.getUserId()).isEqualTo(1L);
        assertThat(revokedToken.getTokenHash()).isEqualTo("hash_value");
        assertThat(revokedToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(revokedToken.isRevoked()).isTrue();
    }
}
