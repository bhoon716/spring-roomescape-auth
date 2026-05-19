package roomescape.domain.reservation.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.common.exception.BusinessException;
import roomescape.domain.reservation.exception.ReservationErrorCode;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.theme.entity.Theme;

class ReservationTest {

    private final Clock fixedClock = Clock.fixed(
            LocalDate.of(2026, 5, 6)
                    .atTime(14, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
            ZoneId.systemDefault()
    );

    private final Theme theme = Theme.of(1L, "워너비", "워너비 테마입니다.", "url");
    private final ReservationTime time = ReservationTime.of(1L, LocalTime.of(15, 0));

    @Test
    @DisplayName("유저가 미래의 예약 일시로 예약을 성공적으로 생성한다.")
    void createByUser_success() {
        // given
        LocalDate futureDate = LocalDate.now(fixedClock).plusDays(1);

        // when
        Reservation reservation = Reservation.createByUser("흑곰", theme, futureDate, time, fixedClock);

        // then
        assertThat(reservation.getUsername()).isEqualTo("흑곰");
        assertThat(reservation.getTheme()).isEqualTo(theme);
        assertThat(reservation.getDate()).isEqualTo(futureDate);
        assertThat(reservation.getTime()).isEqualTo(time);
    }

    @Test
    @DisplayName("유저가 과거 날짜로 예약을 생성하면 예외가 발생한다.")
    void createByUser_throwsException_whenPastDate() {
        // given
        LocalDate pastDate = LocalDate.now(fixedClock).minusDays(1);

        // when & then
        assertThatThrownBy(() -> Reservation.createByUser("흑곰", theme, pastDate, time, fixedClock))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReservationErrorCode.PAST_RESERVATION.getMessage());
    }

    @Test
    @DisplayName("유저가 오늘 날짜의 지난 시간으로 예약을 생성하면 예외가 발생한다.")
    void createByUser_throwsException_whenPastTimeOnToday() {
        // given
        LocalDate today = LocalDate.now(fixedClock);
        ReservationTime pastTime = ReservationTime.of(2L, LocalTime.of(13, 0));

        // when & then
        assertThatThrownBy(() -> Reservation.createByUser("흑곰", theme, today, pastTime, fixedClock))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReservationErrorCode.PAST_RESERVATION.getMessage());
    }

    @Test
    @DisplayName("어드민은 과거 일시로도 예약을 성공적으로 생성한다.")
    void createAdmin_success_withPastDateTime() {
        // given
        LocalDate pastDate = LocalDate.now(fixedClock).minusDays(1);

        // when
        Reservation reservation = Reservation.createAdmin("흑곰", theme, pastDate, time);

        // then
        assertThat(reservation.getUsername()).isEqualTo("흑곰");
        assertThat(reservation.getDate()).isEqualTo(pastDate);
    }

    @Test
    @DisplayName("유저가 미래 일시로 예약을 성공적으로 수정한다.")
    void updateByUser_success() {
        // given
        LocalDate futureDate = LocalDate.now(fixedClock).plusDays(1);
        Reservation reservation = Reservation.of(1L, "흑곰", theme, futureDate, time);

        LocalDate newFutureDate = futureDate.plusDays(1);
        ReservationTime newTime = ReservationTime.of(2L, LocalTime.of(16, 0));

        // when
        Reservation updated = reservation.updateByUser(theme, newFutureDate, newTime, fixedClock);

        // then
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getDate()).isEqualTo(newFutureDate);
        assertThat(updated.getTime()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("유저가 과거 일시로 예약을 수정하려고 하면 예외가 발생한다.")
    void updateByUser_throwsException_whenPastDateTime() {
        // given
        LocalDate futureDate = LocalDate.now(fixedClock).plusDays(1);
        Reservation reservation = Reservation.of(1L, "흑곰", theme, futureDate, time);

        LocalDate pastDate = LocalDate.now(fixedClock).minusDays(1);

        // when & then
        assertThatThrownBy(() -> reservation.updateByUser(theme, pastDate, time, fixedClock))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReservationErrorCode.PAST_RESERVATION.getMessage());
    }

    @Test
    @DisplayName("어드민은 과거 일시로도 예약을 성공적으로 수정한다.")
    void updateByAdmin_success_withPastDateTime() {
        // given
        LocalDate futureDate = LocalDate.now(fixedClock).plusDays(1);
        Reservation reservation = Reservation.of(1L, "흑곰", theme, futureDate, time);

        LocalDate pastDate = LocalDate.now(fixedClock).minusDays(1);

        // when
        Reservation updated = reservation.updateByAdmin(theme, pastDate, time);

        // then
        assertThat(updated.getDate()).isEqualTo(pastDate);
    }

    @Test
    @DisplayName("예약 ID 할당에 성공한다.")
    void assignId_success() {
        // given
        Reservation reservation = Reservation.create("흑곰", theme, LocalDate.now(fixedClock), time);

        // when
        Reservation assigned = reservation.assignId(10L);

        // then
        assertThat(assigned.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("이미 ID가 할당된 예약에 다시 ID를 할당하려고 하면 예외가 발생한다.")
    void assignId_throwsException_whenAlreadyAssigned() {
        // given
        Reservation reservation = Reservation.of(1L, "흑곰", theme, LocalDate.now(fixedClock), time);

        // when & then
        assertThatThrownBy(() -> reservation.assignId(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 id가 할당된 예약입니다.");
    }

    @Test
    @DisplayName("예약 ID가 0 이하이면 예외가 발생한다.")
    void assignId_throwsException_whenInvalidId() {
        // given
        Reservation reservation = Reservation.create("흑곰", theme, LocalDate.now(fixedClock), time);

        // when & then
        assertThatThrownBy(() -> reservation.assignId(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id는 양수여야 합니다.");
    }
}
