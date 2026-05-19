package roomescape.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!hasLoginRequired(handlerMethod)) {
            return true;
        }

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute(LoginMember.SESSION_NAME) == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return true;
    }

    private boolean hasLoginRequired(HandlerMethod handlerMethod) {
        return AnnotationUtils.findAnnotation(
                handlerMethod.getMethod(),
                LoginRequired.class
        ) != null || AnnotationUtils.findAnnotation(
                handlerMethod.getBeanType(),
                LoginRequired.class
        ) != null;
    }
}
