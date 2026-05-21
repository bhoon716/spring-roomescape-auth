package roomescape.domain.auth.entity;

import java.time.LocalDateTime;

public class RefreshToken {

    private final Long id;

    private final Long userId;

    private final String tokenHash;

    private final LocalDateTime expiresAt;

    private final boolean isRevoked;

    private RefreshToken(
            Long id,
            Long userId,
            String tokenHash,
            LocalDateTime expiresAt,
            boolean isRevoked
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.isRevoked = isRevoked;
    }

    public static RefreshToken of(
            Long id,
            Long userId,
            String tokenHash,
            LocalDateTime expiresAt,
            boolean isRevoked
    ) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, isRevoked);
    }

    public static RefreshToken create(Long userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(null, userId, tokenHash, expiresAt, false);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public RefreshToken revoke() {
        return new RefreshToken(
                id,
                userId,
                tokenHash,
                expiresAt,
                true
        );
    }
}
