package roomescape.domain.reservation.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.BusinessException;
import roomescape.common.web.LoginMember;
import roomescape.domain.reservation.entity.Reservation;
import roomescape.domain.reservation.exception.ReservationErrorCode;
import roomescape.domain.reservation.repository.ReservationRepository;
import roomescape.domain.reservation.request.AdminReservationCreateRequest;
import roomescape.domain.reservation.request.ReservationUpdateRequest;
import roomescape.domain.reservation.request.UserReservationCreateRequest;
import roomescape.domain.reservation.response.ReservationResponse;
import roomescape.domain.reservation.response.ReservationsResponse;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.reservationtime.exception.TimeErrorCode;
import roomescape.domain.reservationtime.repository.ReservationTimeRepository;
import roomescape.domain.store.entity.Store;
import roomescape.domain.store.repository.StoreRepository;
import roomescape.domain.theme.entity.Theme;
import roomescape.domain.theme.exception.ThemeErrorCode;
import roomescape.domain.theme.repository.ThemeRepository;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.store.entity.Manager;
import roomescape.domain.store.repository.ManagerRepository;
import roomescape.domain.user.entity.User;
import roomescape.domain.user.entity.UserRole;
import roomescape.domain.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            ThemeRepository themeRepository,
            StoreRepository storeRepository,
            UserRepository userRepository,
            ManagerRepository managerRepository,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.managerRepository = managerRepository;
        this.clock = clock;
    }

    public ReservationsResponse findAllReservations() {
        List<ReservationResponse> reservations = reservationRepository.findAll().stream()
                .map(ReservationResponse::from)
                .toList();

        return new ReservationsResponse(reservations);
    }

    public ReservationsResponse findAllReservationsByStores(List<Long> storeIds) {
        List<ReservationResponse> reservations = reservationRepository.findAllByStoreIdIn(storeIds).stream()
                .map(ReservationResponse::from)
                .toList();

        return new ReservationsResponse(reservations);
    }

    public ReservationsResponse findMyReservations(LoginMember loginMember) {
        List<ReservationResponse> reservations = reservationRepository.findAllByUsername(loginMember.username())
                .stream()
                .map(ReservationResponse::from)
                .toList();

        return new ReservationsResponse(reservations);
    }

    @Transactional
    public ReservationResponse saveReservationByUser(UserReservationCreateRequest request, LoginMember loginMember) {
        Store store = findStoreByIdOrThrow(request.storeId());
        ReservationTime time = findTimeByIdOrThrow(request.timeId());
        Theme theme = findThemeByIdOrThrow(request.themeId());

        validateDuplicateReservation(request.storeId(), request.themeId(), request.date(), request.timeId());

        Reservation reservation = Reservation.createByUser(loginMember.username(), store, theme, request.date(), time, clock);
        Reservation savedReservation = reservationRepository.save(reservation);

        return ReservationResponse.from(savedReservation);
    }

    @Transactional
    public ReservationResponse saveReservationByAdmin(LoginMember loginMember, AdminReservationCreateRequest request) {
        checkStoreAccess(loginMember, request.storeId());

        Store store = findStoreByIdOrThrow(request.storeId());
        ReservationTime time = findTimeByIdOrThrow(request.timeId());
        Theme theme = findThemeByIdOrThrow(request.themeId());

        validateDuplicateReservation(request.storeId(), request.themeId(), request.date(), request.timeId());

        Reservation reservation = Reservation.createAdmin(request.username(), store, theme, request.date(), time);
        Reservation savedReservation = reservationRepository.save(reservation);

        return ReservationResponse.from(savedReservation);
    }

    @Transactional
    public ReservationResponse updateReservationByUser(
            Long reservationId,
            ReservationUpdateRequest request,
            LoginMember loginMember
    ) {
        Store store = findStoreByIdOrThrow(request.storeId());
        ReservationTime newTime = findTimeByIdOrThrow(request.timeId());
        Reservation reservation = findReservationByIdUsernameOrThrow(reservationId, loginMember.username());
        Theme newTheme = findThemeByIdOrThrow(request.themeId());

        validateDuplicateReservationForUpdate(request.storeId(), request.themeId(), request.date(), request.timeId(), reservationId);

        Reservation updatedReservation = reservation.updateByUser(store, newTheme, request.date(), newTime, clock);
        reservationRepository.update(reservationId, updatedReservation);

        return ReservationResponse.from(updatedReservation);
    }

    @Transactional
    public ReservationResponse updateReservationByAdmin(LoginMember loginMember, Long id, ReservationUpdateRequest request) {
        checkReservationAccess(loginMember, id);
        checkStoreAccess(loginMember, request.storeId());

        Store store = findStoreByIdOrThrow(request.storeId());
        Reservation reservation = findReservationByIdOrThrow(id);
        Theme newTheme = findThemeByIdOrThrow(request.themeId());
        ReservationTime newTime = findTimeByIdOrThrow(request.timeId());

        validateDuplicateReservationForUpdate(request.storeId(), request.themeId(), request.date(), request.timeId(), id);

        Reservation updatedReservation = reservation.updateByAdmin(store, newTheme, request.date(), newTime);
        reservationRepository.update(id, updatedReservation);

        return ReservationResponse.from(updatedReservation);
    }

    @Transactional
    public void deleteReservationByUser(Long id, LoginMember loginMember) {
        Reservation reservation = findReservationByIdUsernameOrThrow(id, loginMember.username());
        reservation.validateIsNotInPast(clock);

        deleteByIdOrThrow(id);
    }

    @Transactional
    public void deleteReservationByAdmin(LoginMember loginMember, Long id) {
        checkReservationAccess(loginMember, id);
        deleteByIdOrThrow(id);
    }

    private void validateDuplicateReservation(Long storeId, Long themeId, LocalDate date, Long timeId) {
        if (reservationRepository.existsByStoreIdAndThemeIdAndDateAndTimeId(storeId, themeId, date, timeId)) {
            throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }
    }

    private void validateDuplicateReservationForUpdate(Long storeId, Long themeId, LocalDate date, Long timeId, Long id) {
        if (reservationRepository.existsByStoreIdAndThemeIdAndDateAndTimeIdAndIdNot(storeId, themeId, date, timeId, id)) {
            throw new BusinessException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }
    }

    private Reservation findReservationByIdOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    private Reservation findReservationByIdUsernameOrThrow(Long reservationId, String username) {
        return reservationRepository.findByIdAndUsername(reservationId, username)
                .orElseThrow(() -> new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    private Store findStoreByIdOrThrow(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(roomescape.common.exception.CommonErrorCode.NOT_FOUND));
    }

    private ReservationTime findTimeByIdOrThrow(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new BusinessException(TimeErrorCode.RESERVATION_TIME_NOT_FOUND));
    }

    private Theme findThemeByIdOrThrow(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new BusinessException(ThemeErrorCode.THEME_NOT_FOUND));
    }

    private void deleteByIdOrThrow(Long id) {
        int deletedCount = reservationRepository.deleteById(id);

        if (deletedCount == 0) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
    }

    private void checkStoreAccess(LoginMember loginMember, Long storeId) {
        User user = userRepository.findById(loginMember.id())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (user.getRole() == UserRole.MANAGER) {
            Manager manager = managerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));

            if (storeId == null || !manager.manages(storeId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN);
            }
            return;
        }

        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    private void checkReservationAccess(LoginMember loginMember, Long reservationId) {
        User user = userRepository.findById(loginMember.id())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        if (user.getRole() == UserRole.MANAGER) {
            Manager manager = managerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));

            if (reservationId == null) {
                throw new BusinessException(CommonErrorCode.NOT_FOUND);
            }

            boolean hasAccess = reservationRepository.existsByIdAndStoreIdIn(reservationId, manager.getManagedStoreIds());
            if (!hasAccess) {
                throw new BusinessException(CommonErrorCode.NOT_FOUND);
            }
            return;
        }

        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
}
