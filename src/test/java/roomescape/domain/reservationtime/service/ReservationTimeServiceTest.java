package roomescape.domain.reservationtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import roomescape.common.exception.BusinessException;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.reservationtime.exception.TimeErrorCode;
import roomescape.domain.reservationtime.repository.ReservationTimeRepository;
import roomescape.domain.reservationtime.request.ReservationTimeCreateRequest;
import roomescape.domain.reservationtime.request.ReservationTimeUpdateRequest;
import roomescape.domain.reservationtime.response.ReservationTimeResponse;
import roomescape.domain.reservationtime.response.ReservationTimesResponse;

class ReservationTimeServiceTest {

    private ReservationTimeRepository reservationTimeRepository;
    private ReservationTimeService service;

    @BeforeEach
    void setUp() {
        reservationTimeRepository = mock(ReservationTimeRepository.class);
        service = new ReservationTimeService(reservationTimeRepository);
    }

    @Test
    @DisplayName("findAllReservationTimes 호출 시 모든 예약 시간을 리턴한다")
    void findAllReservationTimes_success() {
        // given
        ReservationTime time1 = ReservationTime.of(1L, LocalTime.of(10, 0));
        ReservationTime time2 = ReservationTime.of(2L, LocalTime.of(12, 0));
        when(reservationTimeRepository.findAll()).thenReturn(List.of(time1, time2));

        // when
        ReservationTimesResponse response = service.findAllReservationTimes();

        // then
        assertThat(response.times()).hasSize(2);
        assertThat(response.times().get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("saveReservationTime 호출 시 정상적으로 저장하고 DTO를 반환한다")
    void saveReservationTime_success() {
        // given
        LocalTime startAt = LocalTime.of(15, 0);
        ReservationTimeCreateRequest request = new ReservationTimeCreateRequest(startAt);
        when(reservationTimeRepository.existsByStartAt(startAt)).thenReturn(false);

        ReservationTime savedTime = ReservationTime.of(10L, startAt);
        when(reservationTimeRepository.save(any(ReservationTime.class))).thenReturn(savedTime);

        // when
        ReservationTimeResponse response = service.saveReservationTime(request);

        // then
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.startAt()).isEqualTo(startAt);
        verify(reservationTimeRepository).save(any(ReservationTime.class));
    }

    @Test
    @DisplayName("이미 존재하는 시간으로 저장 시도 시 DUPLICATE 예외를 던진다")
    void saveReservationTime_fail_duplicate() {
        // given
        LocalTime startAt = LocalTime.of(15, 0);
        ReservationTimeCreateRequest request = new ReservationTimeCreateRequest(startAt);
        when(reservationTimeRepository.existsByStartAt(startAt)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.saveReservationTime(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TimeErrorCode.RESERVATION_TIME_DUPLICATE);
        verify(reservationTimeRepository, never()).save(any(ReservationTime.class));
    }

    @Test
    @DisplayName("updateReservationTime 호출 시 성공적으로 시간을 수정한다")
    void updateReservationTime_success() {
        // given
        LocalTime newTime = LocalTime.of(16, 0);
        ReservationTimeUpdateRequest request = new ReservationTimeUpdateRequest(newTime);
        ReservationTime existingTime = ReservationTime.of(5L, LocalTime.of(14, 0));

        when(reservationTimeRepository.findById(5L)).thenReturn(Optional.of(existingTime));
        when(reservationTimeRepository.existsByStartAtAndIdNot(newTime, 5L)).thenReturn(false);

        // when
        ReservationTimeResponse response = service.updateReservationTime(5L, request);

        // then
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.startAt()).isEqualTo(newTime);
        verify(reservationTimeRepository).update(any(Long.class), any(ReservationTime.class));
    }

    @Test
    @DisplayName("deleteReservationTimeBy 호출 시 정상적으로 삭제한다")
    void deleteReservationTimeBy_success() {
        // given
        when(reservationTimeRepository.deleteById(1L)).thenReturn(1);

        // when
        service.deleteReservationTimeBy(1L);

        // then
        verify(reservationTimeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 시간을 삭제하려고 하면 RESERVATION_TIME_NOT_FOUND 예외를 던진다")
    void deleteReservationTimeBy_fail_not_found() {
        // given
        when(reservationTimeRepository.deleteById(1L)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> service.deleteReservationTimeBy(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TimeErrorCode.RESERVATION_TIME_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 시 외래키 무결성 제약 조건 위반 발생 시 RESERVATION_TIME_DELETE_CONFLICT 예외를 던진다")
    void deleteReservationTimeBy_fail_conflict() {
        // given
        when(reservationTimeRepository.deleteById(1L))
                .thenThrow(new DataIntegrityViolationException("foreign key violation"));

        // when & then
        assertThatThrownBy(() -> service.deleteReservationTimeBy(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TimeErrorCode.RESERVATION_TIME_DELETE_CONFLICT);
    }
}
