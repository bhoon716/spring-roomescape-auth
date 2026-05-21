package roomescape.domain.reservation.response;

import java.time.LocalDate;
import roomescape.domain.reservation.entity.Reservation;
import roomescape.domain.reservationtime.response.ReservationTimeResponse;
import roomescape.domain.store.response.StoreResponse;
import roomescape.domain.theme.response.ThemeResponse;

public record ReservationResponse(
        Long id,
        String username,
        StoreResponse store,
        ThemeResponse theme,
        LocalDate date,
        ReservationTimeResponse time
) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUsername(),
                StoreResponse.from(reservation.getStore()),
                ThemeResponse.from(reservation.getTheme()),
                reservation.getDate(),
                ReservationTimeResponse.from(reservation.getTime())
        );
    }
}
