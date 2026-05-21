package roomescape.domain.store.repository;

import java.util.List;
import java.util.Optional;
import roomescape.domain.store.entity.Store;

public interface StoreRepository {

    List<Store> findAll();

    Optional<Store> findById(Long id);

    Store save(Store store);

    int deleteById(Long id);
}
