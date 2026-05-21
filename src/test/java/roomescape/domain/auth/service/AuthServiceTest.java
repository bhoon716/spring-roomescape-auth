package roomescape.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import roomescape.domain.user.entity.UserRole;
import roomescape.domain.user.exception.UserErrorCode;
import roomescape.domain.user.repository.UserRepository;

class AuthServiceTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);
        clock = Clock.fixed(Instant.parse("2026-05-21T18:00:00Z"), ZoneId.of("UTC"));

        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                clock
        );
    }

    @Test
    @DisplayName("회원가입 성공 시 새 사용자를 생성해 저장하고 성공 DTO를 반환한다")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest("newuser", "password123");
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        
        User savedUser = User.of(1L, "newuser", "encodedPassword", UserRole.USER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 사용자 이름으로 가입하려고 하면 DUPLICATED_USERNAME 예외를 던진다")
    void signup_fail_duplicate_username() {
        // given
        SignupRequest request = new SignupRequest("duplicate", "password123");
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.DUPLICATED_USERNAME);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 시 유효한 액세스/리프레시 토큰 쌍을 생성하고 리프레시 토큰 해시를 저장한다")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("user1", "password123");
        User user = User.of(1L, "user1", "encodedPassword", UserRole.USER);
        
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);

        JwtTokenPair expectedPair = new JwtTokenPair("access", "refresh", "Bearer", 3600, 604800);
        when(jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole().name()))
                .thenReturn(expectedPair);
        when(jwtTokenProvider.hash("refresh")).thenReturn("hashedRefresh");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800L);

        // when
        JwtTokenPair result = authService.login(request);

        // then
        assertThat(result).isEqualTo(expectedPair);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 시 존재하지 않는 사용자 이름이면 USERNAME_OR_PASSWORD_NOT_MATCHES 예외를 던진다")
    void login_fail_username_not_found() {
        // given
        LoginRequest request = new LoginRequest("non_existent", "password123");
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USERNAME_OR_PASSWORD_NOT_MATCHES);
    }

    @Test
    @DisplayName("로그인 시 패스워드가 올바르지 않으면 USERNAME_OR_PASSWORD_NOT_MATCHES 예외를 던진다")
    void login_fail_password_not_matching() {
        // given
        LoginRequest request = new LoginRequest("user1", "wrong_password");
        User user = User.of(1L, "user1", "encodedPassword", UserRole.USER);

        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USERNAME_OR_PASSWORD_NOT_MATCHES);
    }

    @Test
    @DisplayName("올바른 리프레시 토큰으로 재발급 시 기존 토큰을 폐기하고 새 토큰 쌍을 생성해 리턴한다")
    void reissue_success() {
        // given
        String inputToken = "valid-refresh";
        String hashedToken = "hashed-refresh";
        
        when(jwtTokenProvider.hash(inputToken)).thenReturn(hashedToken);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(clock.instant().plusSeconds(604800), ZoneId.of("UTC"));
        RefreshToken activeToken = RefreshToken.of(10L, 1L, hashedToken, expiresAt, false);
        when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(activeToken));
        when(refreshTokenRepository.revokeIfNotRevoked(activeToken)).thenReturn(true);

        User user = User.of(1L, "user1", "encoded", UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        JwtTokenPair expectedPair = new JwtTokenPair("new-access", "new-refresh", "Bearer", 3600, 604800);
        when(jwtTokenProvider.createToken(1L, "user1", "USER")).thenReturn(expectedPair);
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800L);

        // when
        JwtTokenPair result = authService.reissue(inputToken);

        // then
        assertThat(result).isEqualTo(expectedPair);
        verify(refreshTokenRepository).revokeIfNotRevoked(activeToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("재발급 시 만료된 리프레시 토큰이 전달되면 UNAUTHORIZED 예외를 던지고 폐기 처리한다")
    void reissue_fail_expired_token() {
        // given
        String inputToken = "expired-refresh";
        String hashedToken = "hashed-expired";

        when(jwtTokenProvider.hash(inputToken)).thenReturn(hashedToken);

        // 1초 전 만료되도록 설정
        LocalDateTime expiredTime = LocalDateTime.ofInstant(clock.instant().minusSeconds(1), ZoneId.of("UTC"));
        RefreshToken expiredToken = RefreshToken.of(10L, 1L, hashedToken, expiredTime, false);
        when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> authService.reissue(inputToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
        verify(refreshTokenRepository).update(any(RefreshToken.class)); // revoke 처리 업데이트 확인
    }

    @Test
    @DisplayName("이미 폐기된 리프레시 토큰이 전달되면(토큰 탈취 우려) 동일 사용자 모든 토큰을 강제 일괄 무효화 처리하고 UNAUTHORIZED 예외를 던진다")
    void reissue_fail_already_revoked_token() {
        // given
        String inputToken = "stolen-refresh";
        String hashedToken = "hashed-stolen";

        when(jwtTokenProvider.hash(inputToken)).thenReturn(hashedToken);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(clock.instant().plusSeconds(604800), ZoneId.of("UTC"));
        RefreshToken alreadyRevokedToken = RefreshToken.of(10L, 1L, hashedToken, expiresAt, true);
        when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(alreadyRevokedToken));

        // when & then
        assertThatThrownBy(() -> authService.reissue(inputToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
        
        // 동일 사용자 모든 리프레시 토큰을 무효화하는 로직이 호출되었는지 확인
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("로그아웃 호출 시 유효한 리프레시 토큰을 폐기 처리한다")
    void logout_success() {
        // given
        String inputToken = "logout-refresh";
        String hashedToken = "hashed-logout";

        when(jwtTokenProvider.hash(inputToken)).thenReturn(hashedToken);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(clock.instant().plusSeconds(604800), ZoneId.of("UTC"));
        RefreshToken activeToken = RefreshToken.of(10L, 1L, hashedToken, expiresAt, false);
        when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(activeToken));
        when(refreshTokenRepository.revokeIfNotRevoked(activeToken)).thenReturn(true);

        // when
        authService.logout(inputToken);

        // then
        verify(refreshTokenRepository).revokeIfNotRevoked(activeToken);
    }
}
