package roomescape.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import roomescape.domain.theme.entity.Theme;
import roomescape.domain.theme.exception.ThemeErrorCode;
import roomescape.domain.theme.repository.ThemeRepository;

class ReservationServiceTest {

    private ReservationRepository reservationRepository;
    private ReservationTimeRepository reservationTimeRepository;
    private ThemeRepository themeRepository;
    private Clock clock;
    private ReservationService service;

    private Theme sampleTheme;
    private ReservationTime sampleTime;
    private LoginMember loginMember;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        reservationTimeRepository = mock(ReservationTimeRepository.class);
        themeRepository = mock(ThemeRepository.class);
        clock = Clock.fixed(Instant.parse("2026-05-21T18:00:00Z"), ZoneId.of("UTC")); // 2026-05-21 18:00:00 UTC

        service = new ReservationService(
                reservationRepository,
                reservationTimeRepository,
                themeRepository,
                clock
        );

        sampleTheme = Theme.of(1L, "공포테마", "무서운 설명", "http://image.png");
        sampleTime = ReservationTime.of(1L, LocalTime.of(20, 0)); // 20:00
        loginMember = new LoginMember(10L, "user1");
    }

    @Test
    @DisplayName("findAllReservations 호출 시 모든 예약을 조회하여 반환한다")
    void findAllReservations_success() {
        // given
        Reservation reservation = Reservation.of(100L, "user1", sampleTheme, LocalDate.of(2026, 5, 22), sampleTime);
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        // when
        ReservationsResponse response = service.findAllReservations();

        // then
        assertThat(response.reservations()).hasSize(1);
        assertThat(response.reservations().get(0).id()).isEqualTo(100L);
        assertThat(response.reservations().get(0).theme().name()).isEqualTo("공포테마");
    }

    @Test
    @DisplayName("findMyReservations 호출 시 본인의 예약을 조회하여 반환한다")
    void findMyReservations_success() {
        // given
        Reservation reservation = Reservation.of(100L, "user1", sampleTheme, LocalDate.of(2026, 5, 22), sampleTime);
        when(reservationRepository.findAllByUsername("user1")).thenReturn(List.of(reservation));

        // when
        ReservationsResponse response = service.findMyReservations(loginMember);

        // then
        assertThat(response.reservations()).hasSize(1);
        assertThat(response.reservations().get(0).username()).isEqualTo("user1");
    }

    @Test
    @DisplayName("saveReservationByUser 호출 시 예약을 정상적으로 저장한다")
    void saveReservationByUser_success() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 22); // 미래 날짜
        UserReservationCreateRequest request = new UserReservationCreateRequest(1L, date, 1L);

        when(reservationTimeRepository.findById(1L)).thenReturn(Optional.of(sampleTime));
        when(themeRepository.findById(1L)).thenReturn(Optional.of(sampleTheme));
        when(reservationRepository.existsByThemeIdAndDateAndTimeId(1L, date, 1L)).thenReturn(false);

        Reservation savedReservation = Reservation.of(200L, "user1", sampleTheme, date, sampleTime);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        // when
        ReservationResponse response = service.saveReservationByUser(request, loginMember);

        // then
        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.theme().id()).isEqualTo(1L);
        assertThat(response.time().id()).isEqualTo(1L);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("유저 예약 시 예약 시간이 존재하지 않으면 RESERVATION_TIME_NOT_FOUND 예외를 던진다")
    void saveReservationByUser_fail_timeNotFound() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 22);
        UserReservationCreateRequest request = new UserReservationCreateRequest(1L, date, 999L);
        when(reservationTimeRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.saveReservationByUser(request, loginMember))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TimeErrorCode.RESERVATION_TIME_NOT_FOUND);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("유저 예약 시 테마가 존재하지 않으면 THEME_NOT_FOUND 예외를 던진다")
    void saveReservationByUser_fail_themeNotFound() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 22);
        UserReservationCreateRequest request = new UserReservationCreateRequest(999L, date, 1L);
        when(reservationTimeRepository.findById(1L)).thenReturn(Optional.of(sampleTime));
        when(themeRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.saveReservationByUser(request, loginMember))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ThemeErrorCode.THEME_NOT_FOUND);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("유저 예약 시 중복된 예약이 이미 존재하면 DUPLICATE_RESERVATION 예외를 던진다")
    void saveReservationByUser_fail_duplicate() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 22);
        UserReservationCreateRequest request = new UserReservationCreateRequest(1L, date, 1L);
        when(reservationTimeRepository.findById(1L)).thenReturn(Optional.of(sampleTime));
        when(themeRepository.findById(1L)).thenReturn(Optional.of(sampleTheme));
        when(reservationRepository.existsByThemeIdAndDateAndTimeId(1L, date, 1L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.saveReservationByUser(request, loginMember))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.DUPLICATE_RESERVATION);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("유저 예약 시 과거 날짜로 예약하면 PAST_RESERVATION 예외를 던진다")
    void saveReservationByUser_fail_pastReservation() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 20); // 오늘(21일)보다 과거
        UserReservationCreateRequest request = new UserReservationCreateRequest(1L, date, 1L);
        when(reservationTimeRepository.findById(1L)).thenReturn(Optional.of(sampleTime));
        when(themeRepository.findById(1L)).thenReturn(Optional.of(sampleTheme));
        when(reservationRepository.existsByThemeIdAndDateAndTimeId(1L, date, 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.saveReservationByUser(request, loginMember))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.PAST_RESERVATION);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("saveReservationByAdmin 호출 시 어드민이 정상적으로 예약을 저장한다")
    void saveReservationByAdmin_success() {
        // given
        LocalDate date = LocalDate.of(2026, 5, 22);
        AdminReservationCreateRequest request = new AdminReservationCreateRequest("대리인", 1L, date, 1L);

        when(reservationTimeRepository.findById(1L)).thenReturn(Optional.of(sampleTime));
        when(themeRepository.findById(1L)).thenReturn(Optional.of(sampleTheme));
        when(reservationRepository.existsByThemeIdAndDateAndTimeId(1L, date, 1L)).thenReturn(false);

        Reservation savedReservation = Reservation.of(300L, "대리인", sampleTheme, date, sampleTime);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        // when
        ReservationResponse response = service.saveReservationByAdmin(request);

        // then
        assertThat(response.id()).isEqualTo(300L);
        assertThat(response.username()).isEqualTo("대리인");
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("updateReservationByUser 호출 시 본인의 예약을 정상적으로 수정한다")
    void updateReservationByUser_success() {
        // given
        Long reservationId = 100L;
        LocalDate newDate = LocalDate.of(2026, 5, 25);
        ReservationTime newTime = ReservationTime.of(2L, LocalTime.of(16, 0));
        Theme newTheme = Theme.of(2L, "신규테마", "신설명", "url");
        ReservationUpdateRequest request = new ReservationUpdateRequest(2L, newDate, 2L);

        Reservation existingReservation = Reservation.of(reservationId, "user1", sampleTheme, LocalDate.of(2026, 5, 22), sampleTime);

        when(reservationTimeRepository.findById(2L)).thenReturn(Optional.of(newTime));
        when(reservationRepository.findByIdAndUsername(reservationId, "user1")).thenReturn(Optional.of(existingReservation));
        when(themeRepository.findById(2L)).thenReturn(Optional.of(newTheme));
        when(reservationRepository.existsByThemeIdAndDateAndTimeIdAndIdNot(2L, newDate, 2L, reservationId)).thenReturn(false);

        // when
        ReservationResponse response = service.updateReservationByUser(reservationId, request, loginMember);

        // then
        assertThat(response.id()).isEqualTo(reservationId);
        assertThat(response.theme().id()).isEqualTo(2L);
        assertThat(response.time().id()).isEqualTo(2L);
        verify(reservationRepository).update(any(Long.class), any(Reservation.class));
    }

    @Test
    @DisplayName("updateReservationByAdmin 호출 시 어드민이 정상적으로 예약을 수정한다")
    void updateReservationByAdmin_success() {
        // given
        Long reservationId = 100L;
        LocalDate newDate = LocalDate.of(2026, 5, 25);
        ReservationTime newTime = ReservationTime.of(2L, LocalTime.of(16, 0));
        Theme newTheme = Theme.of(2L, "신규테마", "신설명", "url");
        ReservationUpdateRequest request = new ReservationUpdateRequest(2L, newDate, 2L);

        Reservation existingReservation = Reservation.of(reservationId, "user1", sampleTheme, LocalDate.of(2026, 5, 22), sampleTime);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(existingReservation));
        when(themeRepository.findById(2L)).thenReturn(Optional.of(newTheme));
        when(reservationTimeRepository.findById(2L)).thenReturn(Optional.of(newTime));
        when(reservationRepository.existsByThemeIdAndDateAndTimeIdAndIdNot(2L, newDate, 2L, reservationId)).thenReturn(false);

        // when
        ReservationResponse response = service.updateReservationByAdmin(reservationId, request);

        // then
        assertThat(response.id()).isEqualTo(reservationId);
        assertThat(response.theme().id()).isEqualTo(2L);
        assertThat(response.time().id()).isEqualTo(2L);
        verify(reservationRepository).update(any(Long.class), any(Reservation.class));
    }

    @Test
    @DisplayName("deleteReservationByUser 호출 시 본인의 미래 예약을 정상적으로 삭제한다")
    void deleteReservationByUser_success() {
        // given
        Long reservationId = 100L;
        Reservation existingReservation = Reservation.of(reservationId, "user1", sampleTheme, LocalDate.of(2026, 5, 22), sampleTime);

        when(reservationRepository.findByIdAndUsername(reservationId, "user1")).thenReturn(Optional.of(existingReservation));
        when(reservationRepository.deleteById(reservationId)).thenReturn(1);

        // when
        service.deleteReservationByUser(reservationId, loginMember);

        // then
        verify(reservationRepository).deleteById(reservationId);
    }

    @Test
    @DisplayName("deleteReservationByUser 호출 시 과거 예약이면 PAST_RESERVATION 예외를 던진다")
    void deleteReservationByUser_fail_pastReservation() {
        // given
        Long reservationId = 100L;
        // 과거 날짜
        Reservation existingReservation = Reservation.of(reservationId, "user1", sampleTheme, LocalDate.of(2026, 5, 20), sampleTime);

        when(reservationRepository.findByIdAndUsername(reservationId, "user1")).thenReturn(Optional.of(existingReservation));

        // when & then
        assertThatThrownBy(() -> service.deleteReservationByUser(reservationId, loginMember))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.PAST_RESERVATION);

        verify(reservationRepository, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("deleteReservationByAdmin 호출 시 어드민이 정상적으로 예약을 삭제한다")
    void deleteReservationByAdmin_success() {
        // given
        Long reservationId = 100L;
        when(reservationRepository.deleteById(reservationId)).thenReturn(1);

        // when
        service.deleteReservationByAdmin(reservationId);

        // then
        verify(reservationRepository).deleteById(reservationId);
    }

    @Test
    @DisplayName("어드민이 존재하지 않는 예약을 삭제하려고 하면 RESERVATION_NOT_FOUND 예외를 던진다")
    void deleteReservationByAdmin_fail_notFound() {
        // given
        Long reservationId = 999L;
        when(reservationRepository.deleteById(reservationId)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> service.deleteReservationByAdmin(reservationId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
    }
}
