package roomescape.domain.store.repository;

import java.util.Optional;
import roomescape.domain.store.entity.Manager;

public interface ManagerRepository {

    Optional<Manager> findByUserId(Long userId);

    Manager save(Manager manager);
}
