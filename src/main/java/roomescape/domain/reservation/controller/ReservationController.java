package roomescape.domain.reservation.controller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.common.web.LoginRequired;
import roomescape.common.web.LoginUser;
import roomescape.common.web.LoginMember;
import roomescape.domain.reservation.request.ReservationUpdateRequest;
import roomescape.domain.reservation.request.UserReservationCreateRequest;
import roomescape.domain.reservation.response.ReservationResponse;
import roomescape.domain.reservation.response.ReservationsResponse;
import roomescape.domain.reservation.service.ReservationService;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @LoginRequired
    @GetMapping("/me")
    public ResponseEntity<ReservationsResponse> getMyReservations(
            @LoginUser LoginMember loginMember
    ) {
        ReservationsResponse response = reservationService.findMyReservations(loginMember);
        return ResponseEntity.ok(response);
    }

    @LoginRequired
    @PostMapping
    public ResponseEntity<ReservationResponse> save(
            @RequestBody @Valid UserReservationCreateRequest request,
            @LoginUser LoginMember loginMember
    ) {
        ReservationResponse response = reservationService.saveReservationByUser(request, loginMember);
        return ResponseEntity.created(URI.create("/reservations/" + response.id()))
                .body(response);
    }

    @LoginRequired
    @PatchMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long reservationId,
            @RequestBody @Valid ReservationUpdateRequest request,
            @LoginUser LoginMember loginMember
    ) {
        ReservationResponse response = reservationService.updateReservationByUser(reservationId, request, loginMember);
        return ResponseEntity.ok(response);
    }

    @LoginRequired
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable Long reservationId,
            @LoginUser LoginMember loginMember
    ) {
        reservationService.deleteReservationByUser(reservationId, loginMember);
        return ResponseEntity.noContent().build();
    }
}
