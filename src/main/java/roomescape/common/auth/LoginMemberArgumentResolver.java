package roomescape.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

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

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        Object attribute = session.getAttribute(LoginMember.SESSION_NAME);

        if (!(attribute instanceof LoginMember loginMember)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return loginMember;
    }
}
