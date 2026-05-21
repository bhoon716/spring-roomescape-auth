package roomescape.domain.reservation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import roomescape.domain.reservation.entity.Reservation;

public interface ReservationRepository {

    List<Reservation> findAll();

    List<Reservation> findAllByUsername(String username);

    List<Reservation> findAllByStoreIdIn(List<Long> storeIds);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByIdAndUsername(Long id, String username);

    Optional<Reservation> findByIdAndStoreIdIn(Long id, List<Long> storeIds);

    Reservation save(Reservation reservation);

    int update(Long id, Reservation reservation);

    int deleteById(Long id);

    boolean existsByStoreIdAndThemeIdAndDateAndTimeId(Long storeId, Long themeId, LocalDate date, Long timeId);

    boolean existsByStoreIdAndThemeIdAndDateAndTimeIdAndIdNot(Long storeId, Long themeId, LocalDate date, Long timeId, Long id);

    boolean existsByIdAndStoreIdIn(Long id, List<Long> storeIds);
}
