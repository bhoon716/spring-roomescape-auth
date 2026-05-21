package roomescape.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.auth.token.AuthHeaderExtractor;
import roomescape.domain.auth.token.JwtTokenProvider;

class LoginMemberArgumentResolverTest {

    private JwtTokenProvider jwtTokenProvider;
    private AuthHeaderExtractor authHeaderExtractor;
    private LoginMemberArgumentResolver resolver;

    private NativeWebRequest webRequest;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authHeaderExtractor = mock(AuthHeaderExtractor.class);
        resolver = new LoginMemberArgumentResolver(jwtTokenProvider, authHeaderExtractor);

        webRequest = mock(NativeWebRequest.class);
        request = mock(HttpServletRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(request);
    }

    @Test
    @DisplayName("파라미터에 LoginUser 어노테이션이 있고 타입이 LoginMember일 경우 supportsParameter는 true를 반환한다")
    void supportsParameter_success() throws NoSuchMethodException {
        // given
        class DummyController {
            public void testMethod(@LoginUser LoginMember member) {}
        }
        MethodParameter parameter = new MethodParameter(
                DummyController.class.getMethod("testMethod", LoginMember.class), 0
        );

        // when & then
        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    @DisplayName("파라미터에 어노테이션이 없거나 타입이 일치하지 않을 경우 supportsParameter는 false를 반환한다")
    void supportsParameter_fail() throws NoSuchMethodException {
        // given
        class DummyController {
            public void methodWithoutAnnotation(LoginMember member) {}
            public void methodWithWrongType(@LoginUser String text) {}
        }
        
        MethodParameter param1 = new MethodParameter(
                DummyController.class.getMethod("methodWithoutAnnotation", LoginMember.class), 0
        );
        MethodParameter param2 = new MethodParameter(
                DummyController.class.getMethod("methodWithWrongType", String.class), 0
        );

        // when & then
        assertThat(resolver.supportsParameter(param1)).isFalse();
        assertThat(resolver.supportsParameter(param2)).isFalse();
    }

    @Test
    @DisplayName("토큰을 정상 파싱하여 LoginMember 객체를 생성 및 반환한다")
    void resolveArgument_parse_token_success() {
        // given
        String token = "access-token-xyz";
        when(authHeaderExtractor.extractAccessToken(request)).thenReturn(token);

        Claims claims = new DefaultClaims(Map.of("sub", "5", "username", "user5"));
        when(jwtTokenProvider.getClaims(token)).thenReturn(claims);

        // when
        Object result = resolver.resolveArgument(
                mock(MethodParameter.class), 
                mock(ModelAndViewContainer.class), 
                webRequest, 
                mock(WebDataBinderFactory.class)
        );

        // then
        assertThat(result).isEqualTo(new LoginMember(5L, "user5"));
    }

    @Test
    @DisplayName("request가 null일 경우 UNAUTHORIZED 예외를 던진다")
    void resolveArgument_null_request_throws() {
        // given
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> resolver.resolveArgument(
                mock(MethodParameter.class), 
                mock(ModelAndViewContainer.class), 
                webRequest, 
                mock(WebDataBinderFactory.class)
        )).isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
