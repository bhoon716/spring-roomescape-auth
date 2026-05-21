package roomescape.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("createUser 호출 시 기본 권한이 USER이고 ID가 null인 User 객체를 생성한다")
    void createUser_success() {
        // given & when
        User user = User.createUser("testuser", "password123");

        // then
        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getPassword()).isEqualTo("password123");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("of 호출 시 지정된 ID, 이름, 비밀번호, 권한을 가진 User 객체를 생성한다")
    void of_success() {
        // given & when
        User user = User.of(1L, "adminuser", "adminpass", UserRole.ADMIN);

        // then
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("adminuser");
        assertThat(user.getPassword()).isEqualTo("adminpass");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("동일한 ID를 가진 두 User 객체는 equals 비교 시 true를 반환한다")
    void equals_same_id() {
        // given
        User user1 = User.of(1L, "user1", "pass1", UserRole.USER);
        User user2 = User.of(1L, "user2", "pass2", UserRole.USER);

        // when & then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("ID가 다른 두 User 객체는 equals 비교 시 false를 반환한다")
    void equals_different_id() {
        // given
        User user1 = User.of(1L, "user1", "pass1", UserRole.USER);
        User user2 = User.of(2L, "user1", "pass1", UserRole.USER);

        // when & then
        assertThat(user1).isNotEqualTo(user2);
    }
}
