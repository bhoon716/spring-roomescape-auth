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

    public ReservationTime assignId(Long id) {
        validateAssignableId(id);
        return new ReservationTime(id, this.startAt);
    }

    private void validateAssignableId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }

        if (this.id != null) {
            throw new IllegalStateException("이미 id가 할당된 예약 시간입니다.");
        }
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
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (this.id == null) {
            return false;
        }
        ReservationTime time = (ReservationTime) other;
        return Objects.equals(this.id, time.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
