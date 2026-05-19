package roomescape.domain.reservationtime.entity;

import java.time.LocalTime;
import java.util.Objects;

public class ReservationTime {

    private final Long id;

    private final LocalTime startAt;

    private ReservationTime(Long id, LocalTime startAt) {
        this.id = id;
        this.startAt = startAt;
    }

    public static ReservationTime create(LocalTime startAt) {
        return new ReservationTime(null, startAt);
    }

    public static ReservationTime of(Long id, LocalTime startAt) {
        return new ReservationTime(id, startAt);
    }

    public ReservationTime update(LocalTime startAt) {
        return new ReservationTime(this.id, startAt);
    }

    public Long getId() {
        return id;
    }

    public LocalTime getStartAt() {
        return startAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ReservationTime that = (ReservationTime) other;

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
