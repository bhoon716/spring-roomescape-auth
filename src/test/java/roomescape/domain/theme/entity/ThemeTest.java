package roomescape.domain.theme.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ThemeTest {

    @Test
    @DisplayName("테마 ID 할당에 성공한다.")
    void assignId_success() {
        // given
        Theme theme = Theme.create("워너비", "설명", "url");

        // when
        Theme assigned = theme.assignId(1L);

        // then
        assertThat(assigned.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 ID가 할당된 테마에 다시 ID를 할당하려고 하면 예외가 발생한다.")
    void assignId_throwsException_whenAlreadyAssigned() {
        // given
        Theme theme = Theme.of(1L, "워너비", "설명", "url");

        // when & then
        assertThatThrownBy(() -> theme.assignId(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 id가 할당된 테마입니다.");
    }

    @Test
    @DisplayName("테마 ID가 0 이하이면 예외가 발생한다.")
    void assignId_throwsException_whenInvalidId() {
        // given
        Theme theme = Theme.create("워너비", "설명", "url");

        // when & then
        assertThatThrownBy(() -> theme.assignId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id는 양수여야 합니다.");
    }

    @Test
    @DisplayName("테마 정보를 수정한다.")
    void update() {
        // given
        Theme theme = Theme.of(1L, "기존 이름", "기존 설명", "old-url");

        // when
        Theme updated = theme.update("새 이름", "새 설명", "new-url");

        // then
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("새 이름");
        assertThat(updated.getDescription()).isEqualTo("새 설명");
        assertThat(updated.getThumbnailUrl()).isEqualTo("new-url");
    }
}
