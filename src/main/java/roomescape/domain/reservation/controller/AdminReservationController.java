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
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.common.web.LoginMember;
import roomescape.common.web.LoginUser;
import roomescape.domain.reservation.request.AdminReservationCreateRequest;
import roomescape.domain.reservation.request.ReservationUpdateRequest;
import roomescape.domain.reservation.response.ReservationResponse;
import roomescape.domain.reservation.response.ReservationsResponse;
import roomescape.domain.reservation.service.ReservationService;
import roomescape.domain.store.entity.Manager;
import roomescape.domain.store.repository.ManagerRepository;
import roomescape.domain.user.entity.User;
import roomescape.domain.user.entity.UserRole;
import roomescape.domain.user.repository.UserRepository;

@RestController
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;

    public AdminReservationController(ReservationService reservationService,
                                     UserRepository userRepository,
                                     ManagerRepository managerRepository) {
        this.reservationService = reservationService;
        this.userRepository = userRepository;
        this.managerRepository = managerRepository;
    }

    @GetMapping
    public ResponseEntity<ReservationsResponse> findAll(@LoginUser LoginMember loginMember) {
        User user = userRepository.findById(loginMember.id())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        if (user.getRole() == UserRole.ADMIN) {
            ReservationsResponse reservations = reservationService.findAllReservations();
            return ResponseEntity.ok(reservations);
        }

        if (user.getRole() == UserRole.MANAGER) {
            Manager manager = managerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
            ReservationsResponse reservations = reservationService.findAllReservationsByStores(manager.getManagedStoreIds());
            return ResponseEntity.ok(reservations);
        }

        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> save(
            @LoginUser LoginMember loginMember,
            @RequestBody @Valid AdminReservationCreateRequest request
    ) {
        ReservationResponse response = reservationService.saveReservationByAdmin(loginMember, request);
        return ResponseEntity.created(URI.create("/reservations/" + response.id()))
                .body(response);
    }

    @PatchMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> update(
            @LoginUser LoginMember loginMember,
            @PathVariable Long reservationId,
            @RequestBody @Valid ReservationUpdateRequest request
    ) {
        ReservationResponse response = reservationService.updateReservationByAdmin(loginMember, reservationId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> deleteById(
            @LoginUser LoginMember loginMember,
            @PathVariable Long reservationId
    ) {
        reservationService.deleteReservationByAdmin(loginMember, reservationId);
        return ResponseEntity.noContent().build();
    }
}
