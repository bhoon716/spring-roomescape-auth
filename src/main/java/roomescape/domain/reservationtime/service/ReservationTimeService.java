package roomescape.domain.reservationtime.service;

import java.time.LocalTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.BusinessException;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.reservationtime.exception.TimeErrorCode;
import roomescape.domain.reservationtime.repository.ReservationTimeRepository;
import roomescape.domain.reservationtime.request.ReservationTimeCreateRequest;
import roomescape.domain.reservationtime.request.ReservationTimeUpdateRequest;
import roomescape.domain.reservationtime.response.ReservationTimeResponse;
import roomescape.domain.reservationtime.response.ReservationTimesResponse;

@Service
@Transactional(readOnly = true)
public class ReservationTimeService {

    private final ReservationTimeRepository reservationTimeRepository;

    public ReservationTimeService(ReservationTimeRepository reservationTimeRepository) {
        this.reservationTimeRepository = reservationTimeRepository;
    }

    public ReservationTimesResponse findAllReservationTimes() {
        List<ReservationTimeResponse> times = reservationTimeRepository.findAll().stream()
                .map(ReservationTimeResponse::from)
                .toList();
        return new ReservationTimesResponse(times);
    }

    @Transactional
    public ReservationTimeResponse saveReservationTime(ReservationTimeCreateRequest request) {
        validateReservationTimeNotDuplicated(request.startAt());

        ReservationTime reservationTime = ReservationTime.create(request.startAt());
        ReservationTime savedTime = reservationTimeRepository.save(reservationTime);

        return ReservationTimeResponse.from(savedTime);
    }

    @Transactional
    public ReservationTimeResponse updateReservationTime(Long id, ReservationTimeUpdateRequest request) {
        ReservationTime reservationTime = findTimeByIdOrThrow(id);
        validateReservationTimeNotDuplicated(request.startAt(), id);

        ReservationTime updatedTime = reservationTime.update(request.startAt());
        reservationTimeRepository.update(id, updatedTime);

        return ReservationTimeResponse.from(updatedTime);
    }

    @Transactional
    public void deleteReservationTimeBy(Long id) {
        try {
            int deletedCount = reservationTimeRepository.deleteById(id);
            if (deletedCount == 0) {
                throw new BusinessException(TimeErrorCode.RESERVATION_TIME_NOT_FOUND);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(TimeErrorCode.RESERVATION_TIME_DELETE_CONFLICT, exception);
        }
    }

    private ReservationTime findTimeByIdOrThrow(Long id) {
        return reservationTimeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(TimeErrorCode.RESERVATION_TIME_NOT_FOUND));
    }

    private void validateReservationTimeNotDuplicated(LocalTime startAt) {
        if (reservationTimeRepository.existsByStartAt(startAt)) {
            throw new BusinessException(TimeErrorCode.RESERVATION_TIME_DUPLICATE);
        }
    }

    private void validateReservationTimeNotDuplicated(LocalTime startAt, Long excludedId) {
        if (reservationTimeRepository.existsByStartAtAndIdNot(startAt, excludedId)) {
            throw new BusinessException(TimeErrorCode.RESERVATION_TIME_DUPLICATE);
        }
    }
}
