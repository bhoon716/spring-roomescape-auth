package roomescape.domain.reservationtime.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationTimeTest {

    @Test
    @DisplayName("create 호출 시 ID가 null이고 지정한 시간을 가진 ReservationTime 객체를 생성한다")
    void create_success() {
        // given
        LocalTime startAt = LocalTime.of(14, 30);

        // when
        ReservationTime time = ReservationTime.create(startAt);

        // then
        assertThat(time.getId()).isNull();
        assertThat(time.getStartAt()).isEqualTo(startAt);
    }

    @Test
    @DisplayName("update 호출 시 기존 ID를 유지하며 시간만 변경된 새 ReservationTime 객체를 반환한다")
    void update_success() {
        // given
        ReservationTime time = ReservationTime.of(5L, LocalTime.of(10, 0));

        // when
        ReservationTime updated = time.update(LocalTime.of(12, 0));

        // then
        assertThat(updated).isNotSameAs(time);
        assertThat(updated.getId()).isEqualTo(5L);
        assertThat(updated.getStartAt()).isEqualTo(LocalTime.of(12, 0));
    }
}
