package roomescape.domain.auth.response;

import roomescape.domain.user.entity.User;

public record LoginResponse(Long id, String username, String role) {

    public static LoginResponse from(User user) {
        return new LoginResponse(user.getId(), user.getUsername(), user.getRole().name());
    }
}
