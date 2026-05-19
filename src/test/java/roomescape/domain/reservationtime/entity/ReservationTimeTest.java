package roomescape.domain.reservationtime.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationTimeTest {

    @Test
    @DisplayName("예약 시간 ID 할당에 성공한다.")
    void assignId_success() {
        // given
        ReservationTime time = ReservationTime.create(LocalTime.of(10, 0));

        // when
        ReservationTime assigned = time.assignId(1L);

        // then
        assertThat(assigned.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 ID가 할당된 예약 시간에 다시 ID를 할당하려고 하면 예외가 발생한다.")
    void assignId_throwsException_whenAlreadyAssigned() {
        // given
        ReservationTime time = ReservationTime.of(1L, LocalTime.of(10, 0));

        // when & then
        assertThatThrownBy(() -> time.assignId(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 id가 할당된 예약 시간입니다.");
    }

    @Test
    @DisplayName("예약 시간 ID가 0 이하이면 예외가 발생한다.")
    void assignId_throwsException_whenInvalidId() {
        // given
        ReservationTime time = ReservationTime.create(LocalTime.of(10, 0));

        // when & then
        assertThatThrownBy(() -> time.assignId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id는 양수여야 합니다.");
    }

    @Test
    @DisplayName("예약 시간을 수정한다.")
    void update() {
        // given
        ReservationTime time = ReservationTime.of(1L, LocalTime.of(10, 0));

        // when
        ReservationTime updated = time.update(LocalTime.of(11, 0));

        // then
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getStartAt()).isEqualTo(LocalTime.of(11, 0));
    }
}
