package roomescape.domain.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.auth.properties.AuthTokenProperties;

class AuthHeaderExtractorTest {

    private AuthHeaderExtractor extractor;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        AuthTokenProperties properties = new AuthTokenProperties();
        properties.setType("Bearer");
        extractor = new AuthHeaderExtractor(properties);

        request = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이 들어 있는 Authorization 헤더 검증 시 성공적으로 토큰만 추출하여 반환한다")
    void extractAccessToken_success() {
        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer actual_jwt_token_value");

        // when
        String token = extractor.extractAccessToken(request);

        // then
        assertThat(token).isEqualTo("actual_jwt_token_value");
    }

    @Test
    @DisplayName("Authorization 헤더가 비어 있거나 없을 경우 UNAUTHORIZED 예외를 던진다")
    void extractAccessToken_missing_header_throws() {
        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> extractor.extractAccessToken(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Authorization 헤더의 접두사가 Bearer가 아닐 경우 UNAUTHORIZED 예외를 던진다")
    void extractAccessToken_invalid_prefix_throws() {
        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic plain_auth_value");

        // when & then
        assertThatThrownBy(() -> extractor.extractAccessToken(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Bearer 접두사는 있으나 실제 토큰 값이 비어 있을 경우 UNAUTHORIZED 예외를 던진다")
    void extractAccessToken_empty_token_throws() {
        // given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer ");

        // when & then
        assertThatThrownBy(() -> extractor.extractAccessToken(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
