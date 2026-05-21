package roomescape.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Pbkdf2PasswordEncoderTest {

    private Pbkdf2PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new Pbkdf2PasswordEncoder();
    }

    @Test
    @DisplayName("비밀번호 암호화 시 PBKDF2 포맷의 암호화된 문자열을 성공적으로 생성한다")
    void encode_success() {
        // given
        String rawPassword = "securePassword123";

        // when
        String encoded = passwordEncoder.encode(rawPassword);

        // then
        assertThat(encoded).isNotBlank();
        assertThat(encoded.split("\\$")).hasSize(4);
        assertThat(encoded).startsWith("PBKDF2WithHmacSHA256$");
    }

    @Test
    @DisplayName("공백이나 null 비밀번호 암호화 시도 시 IllegalStateException 예외를 던진다")
    void encode_blank_or_null_throws() {
        assertThatThrownBy(() -> passwordEncoder.encode(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("암호화할 비밀번호는 공백일 수 없습니다.");

        assertThatThrownBy(() -> passwordEncoder.encode(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("암호화할 비밀번호는 공백일 수 없습니다.");
    }

    @Test
    @DisplayName("올바른 원본 비밀번호와 암호화된 비밀번호를 matches로 비교하면 true를 반환한다")
    void matches_success() {
        // given
        String rawPassword = "myPassword!@#";
        String encoded = passwordEncoder.encode(rawPassword);

        // when
        boolean isMatch = passwordEncoder.matches(rawPassword, encoded);

        // then
        assertThat(isMatch).isTrue();
    }

    @Test
    @DisplayName("틀린 원본 비밀번호와 암호화된 비밀번호를 matches로 비교하면 false를 반환한다")
    void matches_fail_different_password() {
        // given
        String rawPassword = "myPassword!@#";
        String wrongPassword = "wrongPassword";
        String encoded = passwordEncoder.encode(rawPassword);

        // when
        boolean isMatch = passwordEncoder.matches(wrongPassword, encoded);

        // then
        assertThat(isMatch).isFalse();
    }

    @Test
    @DisplayName("비정상적인 형식의 암호화 비밀번호와 matches 비교 시 false를 반환한다")
    void matches_fail_invalid_format() {
        // given
        String rawPassword = "myPassword!@#";
        String invalidEncoded = "invalid_format_string";

        // when
        boolean isMatch = passwordEncoder.matches(rawPassword, invalidEncoded);

        // then
        assertThat(isMatch).isFalse();
    }
}
