package roomescape.common.auth;

public record LoginMember(
        Long id,
        String username
) {
    public static final String SESSION_NAME = "loginMember";
}
