package roomescape.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.BusinessException;
import roomescape.common.security.PasswordEncoder;
import roomescape.domain.user.entity.User;
import roomescape.domain.user.exception.UserErrorCode;
import roomescape.domain.user.repository.UserRepository;
import roomescape.domain.user.request.LoginRequest;
import roomescape.domain.user.request.SignupRequest;
import roomescape.domain.user.response.LoginResponse;
import roomescape.domain.user.response.SignupResponse;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USERNAME_OR_PASSWORD_NOT_MATCHES));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.USERNAME_OR_PASSWORD_NOT_MATCHES);
        }

        return LoginResponse.from(user);
    }
}
