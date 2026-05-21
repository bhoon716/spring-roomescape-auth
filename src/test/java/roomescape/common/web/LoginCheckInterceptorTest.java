package roomescape.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.auth.token.AuthHeaderExtractor;
import roomescape.domain.auth.token.JwtTokenProvider;

class LoginCheckInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private AuthHeaderExtractor authHeaderExtractor;
    private LoginCheckInterceptor interceptor;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authHeaderExtractor = mock(AuthHeaderExtractor.class);
        interceptor = new LoginCheckInterceptor(jwtTokenProvider, authHeaderExtractor);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("Handler가 HandlerMethod가 아닐 경우 인터셉터 검증을 건너뛰고 true를 반환한다")
    void preHandle_not_handler_method_returns_true() {
        // given
        Object plainHandler = new Object();

        // when
        boolean result = interceptor.preHandle(request, response, plainHandler);

        // then
        assertThat(result).isTrue();
        verifyNoInteractions(jwtTokenProvider, authHeaderExtractor);
    }

    @Test
    @DisplayName("HandlerMethod에 LoginRequired 어노테이션이 없을 경우 검증을 건너뛰고 true를 반환한다")
    void preHandle_no_annotation_returns_true() throws NoSuchMethodException {
        // given
        class TestController {
            public void noAnnotationMethod() {}
        }
        HandlerMethod handlerMethod = new HandlerMethod(
                new TestController(), 
                TestController.class.getMethod("noAnnotationMethod")
        );

        // when
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(result).isTrue();
        verifyNoInteractions(jwtTokenProvider, authHeaderExtractor);
    }

    @Test
    @DisplayName("LoginRequired 어노테이션이 있고 토큰이 유효할 경우 로그인 사용자 정보를 request attribute에 저장하고 true를 반환한다")
    void preHandle_authorized_success() throws NoSuchMethodException {
        // given
        class TestController {
            @LoginRequired
            public void loginRequiredMethod() {}
        }
        HandlerMethod handlerMethod = new HandlerMethod(
                new TestController(), 
                TestController.class.getMethod("loginRequiredMethod")
        );

        String token = "valid-token";
        when(authHeaderExtractor.extractAccessToken(request)).thenReturn(token);

        Claims claims = new DefaultClaims(Map.of("sub", "10", "username", "user10"));
        when(jwtTokenProvider.getClaims(token)).thenReturn(claims);

        // when
        boolean result = interceptor.preHandle(request, response, handlerMethod);

        // then
        assertThat(result).isTrue();
        verify(request).setAttribute("loginMember", new LoginMember(10L, "user10"));
    }

    @Test
    @DisplayName("LoginRequired 어노테이션이 있으나 토큰 검증에 실패할 경우 UNAUTHORIZED 예외를 던진다")
    void preHandle_unauthorized_throws() throws NoSuchMethodException {
        // given
        class TestController {
            @LoginRequired
            public void loginRequiredMethod() {}
        }
        HandlerMethod handlerMethod = new HandlerMethod(
                new TestController(), 
                TestController.class.getMethod("loginRequiredMethod")
        );

        String token = "invalid-token";
        when(authHeaderExtractor.extractAccessToken(request)).thenReturn(token);
        when(jwtTokenProvider.getClaims(token))
                .thenThrow(new BusinessException(CommonErrorCode.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
