package roomescape.domain.theme.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThemeTest {

    @Test
    @DisplayName("create 호출 시 ID가 null인 Theme 객체를 올바르게 생성한다")
    void create_success() {
        // when
        Theme theme = Theme.create("공포의 방", "정말 무서운 방탈출 테마", "url_path");

        // then
        assertThat(theme.getId()).isNull();
        assertThat(theme.getName()).isEqualTo("공포의 방");
        assertThat(theme.getDescription()).isEqualTo("정말 무서운 방탈출 테마");
        assertThat(theme.getThumbnailUrl()).isEqualTo("url_path");
    }

    @Test
    @DisplayName("update 호출 시 기존 ID를 유지한 채 필드 값들만 수정된 새 Theme 객체를 반환한다")
    void update_success() {
        // given
        Theme theme = Theme.of(1L, "공포의 방", "무서움", "thumb");

        // when
        Theme updated = theme.update("행복한 방", "행복함", "new_thumb");

        // then
        assertThat(updated).isNotSameAs(theme);
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("행복한 방");
        assertThat(updated.getDescription()).isEqualTo("행복함");
        assertThat(updated.getThumbnailUrl()).isEqualTo("new_thumb");
    }
}
