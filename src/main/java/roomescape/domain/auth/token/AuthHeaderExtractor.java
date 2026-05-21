package roomescape.domain.auth.token;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.auth.properties.AuthTokenProperties;

@Component
public class AuthHeaderExtractor {

    private final String authorizationScheme;

    public AuthHeaderExtractor(AuthTokenProperties authTokenProperties) {
        this.authorizationScheme = authTokenProperties.getType();
    }

    public String extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        String prefix = authorizationScheme + " ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        String accessToken = authorizationHeader.substring(prefix.length());
        if (accessToken.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return accessToken;
    }
}
