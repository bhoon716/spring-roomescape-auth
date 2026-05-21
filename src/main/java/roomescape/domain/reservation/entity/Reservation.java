package roomescape.domain.reservation.entity;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import roomescape.common.exception.BusinessException;
import roomescape.domain.reservation.exception.ReservationErrorCode;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.store.entity.Store;
import roomescape.domain.theme.entity.Theme;

public class Reservation {

    private final Long id;

    private final String username;

    private final Store store;

    private final Theme theme;

    private final LocalDate date;

    private final ReservationTime time;

    private Reservation(Long id, String username, Store store, Theme theme, LocalDate date, ReservationTime time) {
        this.id = id;
        this.username = username;
        this.store = store;
        this.theme = theme;
        this.date = date;
        this.time = time;
    }

    public static Reservation createByUser(String username, Store store, Theme theme, LocalDate date, ReservationTime time,
                                           Clock clock) {
        Reservation reservation = new Reservation(null, username, store, theme, date, time);
        reservation.validateIsNotInPast(clock);
        return reservation;
    }

    public static Reservation createAdmin(String username, Store store, Theme theme, LocalDate date, ReservationTime time) {
        return new Reservation(null, username, store, theme, date, time);
    }

    public static Reservation of(Long id, String username, Store store, Theme theme, LocalDate date, ReservationTime time) {
        return new Reservation(id, username, store, theme, date, time);
    }

    public Reservation updateByUser(Store store, Theme theme, LocalDate date, ReservationTime time, Clock clock) {
        this.validateIsNotInPast(clock);
        Reservation newReservation = new Reservation(this.id, this.username, store, theme, date, time);
        newReservation.validateIsNotInPast(clock);
        return newReservation;
    }

    public Reservation updateByAdmin(Store store, Theme theme, LocalDate date, ReservationTime time) {
        return new Reservation(this.id, this.username, store, theme, date, time);
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

    public Store getStore() {
        return store;
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
        Reservation that = (Reservation) other;

        if (this.id == null || that.id == null) {
            return false;
        }

        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return System.identityHashCode(this);
        }
        return id.hashCode();
    }
}
