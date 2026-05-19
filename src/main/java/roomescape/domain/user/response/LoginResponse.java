package roomescape.domain.user.response;

import roomescape.domain.user.entity.User;

public record LoginResponse(Long id, String username) {

    public static LoginResponse from(User user) {
        return new LoginResponse(user.getId(), user.getUsername());
    }
}
