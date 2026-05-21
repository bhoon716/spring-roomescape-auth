package roomescape.domain.auth.repository;

import java.util.Optional;
import roomescape.domain.auth.entity.RefreshToken;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    void update(RefreshToken refreshToken);

    boolean revokeIfNotRevoked(RefreshToken refreshToken);

    void revokeAllByUserId(Long userId);
}
