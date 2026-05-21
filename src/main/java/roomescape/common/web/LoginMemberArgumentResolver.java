package roomescape.common.web;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.auth.token.AuthHeaderExtractor;
import roomescape.domain.auth.token.JwtTokenProvider;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthHeaderExtractor authHeaderExtractor;

    public LoginMemberArgumentResolver(
            JwtTokenProvider jwtTokenProvider,
            AuthHeaderExtractor authHeaderExtractor
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authHeaderExtractor = authHeaderExtractor;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasLoginUserAnnotation = parameter.hasParameterAnnotation(LoginUser.class);
        boolean isLoginMemberType = LoginMember.class.isAssignableFrom(parameter.getParameterType());

        return hasLoginUserAnnotation && isLoginMemberType;
    }

    @Override
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        if (request == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        LoginMember loginMember = (LoginMember) request.getAttribute("loginMember");
        if (loginMember != null) {
            return loginMember;
        }

        String accessToken = authHeaderExtractor.extractAccessToken(request);
        Claims claims = jwtTokenProvider.getClaims(accessToken);

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        return new LoginMember(userId, username);
    }
}
