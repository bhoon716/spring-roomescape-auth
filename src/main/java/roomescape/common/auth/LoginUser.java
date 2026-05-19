package roomescape.common.auth;

public record LoginUser(
        Long id,
        String username
) {
    public static final String SESSION_NAME = "loginUser";
}
