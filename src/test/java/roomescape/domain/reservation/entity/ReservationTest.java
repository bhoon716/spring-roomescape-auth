package roomescape.domain.reservation.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.common.exception.BusinessException;
import roomescape.domain.reservation.exception.ReservationErrorCode;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.store.entity.Store;
import roomescape.domain.theme.entity.Theme;

class ReservationTest {

    private Store dummyStore;
    private Theme dummyTheme;
    private ReservationTime dummyTime;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        dummyStore = new Store(1L, "강남점");
        dummyTheme = Theme.of(1L, "테마", "설명", "url");
        dummyTime = ReservationTime.of(1L, LocalTime.of(15, 0));
        // 현재 시간을 2026-05-21T18:00:00Z 로 고정
        fixedClock = Clock.fixed(Instant.parse("2026-05-21T18:00:00Z"), ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("createByUser 호출 시 미래의 날짜이면 예약을 성공적으로 생성한다")
    void createByUser_future_date_success() {
        // given
        LocalDate futureDate = LocalDate.now(fixedClock).plusDays(1);

        // when
        Reservation reservation = Reservation.createByUser("user1", dummyStore, dummyTheme, futureDate, dummyTime, fixedClock);

        // then
        assertThat(reservation).isNotNull();
        assertThat(reservation.getUsername()).isEqualTo("user1");
        assertThat(reservation.getDate()).isEqualTo(futureDate);
    }

    @Test
    @DisplayName("createByUser 호출 시 오늘 날짜이고 시간도 미래이면 예약을 성공적으로 생성한다")
    void createByUser_today_future_time_success() {
        // given
        LocalDate today = LocalDate.now(fixedClock);
        // 고정 시간이 18:00 이므로, 18:30 예약은 성공해야 함
        ReservationTime futureTime = ReservationTime.of(2L, LocalTime.of(18, 30));

        // when
        Reservation reservation = Reservation.createByUser("user1", dummyStore, dummyTheme, today, futureTime, fixedClock);

        // then
        assertThat(reservation).isNotNull();
        assertThat(reservation.getTime().getStartAt()).isEqualTo(LocalTime.of(18, 30));
    }

    @Test
    @DisplayName("createByUser 호출 시 과거의 날짜이면 PAST_RESERVATION 예외를 던진다")
    void createByUser_past_date_throws() {
        // given
        LocalDate pastDate = LocalDate.now(fixedClock).minusDays(1);

        // when & then
        assertThatThrownBy(() -> Reservation.createByUser("user1", dummyStore, dummyTheme, pastDate, dummyTime, fixedClock))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.PAST_RESERVATION);
    }

    @Test
    @DisplayName("createByUser 호출 시 오늘이지만 이미 지나간 시간이면 PAST_RESERVATION 예외를 던진다")
    void createByUser_today_past_time_throws() {
        // given
        LocalDate today = LocalDate.now(fixedClock);
        // 고정 시간이 18:00 이므로, 17:30 예약은 실패해야 함
        ReservationTime pastTime = ReservationTime.of(2L, LocalTime.of(17, 30));

        // when & then
        assertThatThrownBy(() -> Reservation.createByUser("user1", dummyStore, dummyTheme, today, pastTime, fixedClock))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.PAST_RESERVATION);
    }

    @Test
    @DisplayName("createAdmin 호출 시에는 날짜/시간 검증 없이 예약을 무조건 생성한다")
    void createAdmin_success() {
        // given
        LocalDate pastDate = LocalDate.now(fixedClock).minusDays(5);

        // when
        Reservation reservation = Reservation.createAdmin("admin", dummyStore, dummyTheme, pastDate, dummyTime);

        // then
        assertThat(reservation.getDate()).isEqualTo(pastDate);
    }
}
