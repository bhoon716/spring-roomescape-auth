package roomescape.domain.reservation.entity;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import roomescape.common.exception.BusinessException;
import roomescape.domain.reservation.exception.ReservationErrorCode;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.theme.entity.Theme;

public class Reservation {

    private final Long id;

    private final String username;

    private final Theme theme;

    private final LocalDate date;

    private final ReservationTime time;

    private Reservation(Long id, String username, Theme theme, LocalDate date, ReservationTime time) {
        this.id = id;
        this.username = username;
        this.theme = theme;
        this.date = date;
        this.time = time;
    }

    public static Reservation create(String username, Theme theme, LocalDate date, ReservationTime time) {
        return new Reservation(null, username, theme, date, time);
    }

    public static Reservation createByUser(String username, Theme theme, LocalDate date, ReservationTime time,
                                           Clock clock) {
        Reservation reservation = new Reservation(null, username, theme, date, time);
        reservation.validateIsNotInPast(clock);
        return reservation;
    }

    public static Reservation createAdmin(String username, Theme theme, LocalDate date, ReservationTime time) {
        return new Reservation(null, username, theme, date, time);
    }

    public static Reservation of(Long id, String username, Theme theme, LocalDate date, ReservationTime time) {
        return new Reservation(id, username, theme, date, time);
    }

    public Reservation assignId(Long id) {
        validateAssignableId(id);
        return new Reservation(id, this.username, this.theme, this.date, this.time);
    }

    private void validateAssignableId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }

        if (this.id != null) {
            throw new IllegalStateException("이미 id가 할당된 예약입니다.");
        }
    }

    public Reservation updateByUser(Theme theme, LocalDate date, ReservationTime time, Clock clock) {
        this.validateIsNotInPast(clock);
        Reservation newReservation = new Reservation(this.id, this.username, theme, date, time);
        newReservation.validateIsNotInPast(clock);
        return newReservation;
    }

    public Reservation updateByAdmin(Theme theme, LocalDate date, ReservationTime time) {
        return new Reservation(this.id, this.username, theme, date, time);
    }

    public void validateIsNotInPast(Clock clock) {
        LocalDate nowDate = LocalDate.now(clock);
        LocalTime nowTime = LocalTime.now(clock);

        if (date.isBefore(nowDate)) {
            throw new BusinessException(ReservationErrorCode.PAST_RESERVATION);
        }

        if (date.isEqual(nowDate) && time.getStartAt().isBefore(nowTime)) {
            throw new BusinessException(ReservationErrorCode.PAST_RESERVATION);
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Theme getTheme() {
        return theme;
    }

    public LocalDate getDate() {
        return date;
    }

    public ReservationTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (this.id == null) {
            return false;
        }
        Reservation reservation = (Reservation) other;
        return Objects.equals(this.id, reservation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
