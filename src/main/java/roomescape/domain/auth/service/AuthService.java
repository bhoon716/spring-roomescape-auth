package roomescape.domain.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.common.security.PasswordEncoder;
import roomescape.domain.auth.entity.RefreshToken;
import roomescape.domain.auth.repository.RefreshTokenRepository;
import roomescape.domain.auth.request.LoginRequest;
import roomescape.domain.auth.request.SignupRequest;
import roomescape.domain.auth.response.SignupResponse;
import roomescape.domain.auth.token.JwtTokenPair;
import roomescape.domain.auth.token.JwtTokenProvider;
import roomescape.domain.user.entity.User;
import roomescape.domain.user.exception.UserErrorCode;
import roomescape.domain.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(UserErrorCode.DUPLICATED_USERNAME);
        }

        User user = User.createUser(request.username(), passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);

        return SignupResponse.from(savedUser);
    }

    @Transactional
    public JwtTokenPair login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USERNAME_OR_PASSWORD_NOT_MATCHES));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.USERNAME_OR_PASSWORD_NOT_MATCHES);
        }

        JwtTokenPair tokenPair = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole().name());

        saveRefreshToken(user.getId(), tokenPair.refreshToken());

        return tokenPair;
    }

    @Transactional
    public JwtTokenPair reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        String tokenHash = jwtTokenProvider.hash(refreshToken);
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        validateRevoke(refreshTokenEntity);
        validateExpire(refreshTokenEntity);
        revokeOrThrow(refreshTokenEntity);

        User user = userRepository.findById(refreshTokenEntity.getUserId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        JwtTokenPair tokenPair = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole().name());
        saveRefreshToken(user.getId(), tokenPair.refreshToken());

        return tokenPair;
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        String tokenHash = jwtTokenProvider.hash(refreshToken);
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        validateRevoke(refreshTokenEntity);
        validateExpire(refreshTokenEntity);
        revokeOrThrow(refreshTokenEntity);
    }

    private void validateRevoke(RefreshToken refreshTokenEntity) {
        if (refreshTokenEntity.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(refreshTokenEntity.getUserId());
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void validateExpire(RefreshToken refreshTokenEntity) {
        if (refreshTokenEntity.isExpired(LocalDateTime.ofInstant(clock.instant(), UTC))) {
            refreshTokenRepository.update(refreshTokenEntity.revoke());
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void revokeOrThrow(RefreshToken refreshTokenEntity) {
        if (!refreshTokenRepository.revokeIfNotRevoked(refreshTokenEntity)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        String tokenHash = jwtTokenProvider.hash(refreshToken);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.ofInstant(
                clock.instant().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration()),
                UTC
        );

        RefreshToken refreshTokenEntity = RefreshToken.create(
                userId,
                tokenHash,
                refreshTokenExpiresAt
        );

        refreshTokenRepository.save(refreshTokenEntity);
    }
}
