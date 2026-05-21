package roomescape.domain.auth.response;

import roomescape.domain.user.entity.User;

public record SignupResponse(Long id, String username) {

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getUsername());
    }
}
