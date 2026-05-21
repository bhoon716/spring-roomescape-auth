package roomescape.domain.user.repository;

import java.util.Optional;
import roomescape.domain.user.entity.User;

public interface UserRepository {

    Optional<User> findByUsername(String username);

    User save(User user);

    boolean existsByUsername(String username);

    Optional<User> findById(Long id);
}
