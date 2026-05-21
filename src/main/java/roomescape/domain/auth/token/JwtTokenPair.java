package roomescape.domain.auth.token;

public record JwtTokenPair(
        String accessToken,
        String refreshToken,
        String type,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {

    public String getAuthorizationHeader() {
        return String.format("%s %s", type, accessToken);
    }
}
