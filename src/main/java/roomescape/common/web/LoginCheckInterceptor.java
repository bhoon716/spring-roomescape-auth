package roomescape.common.web;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import roomescape.domain.auth.token.AuthHeaderExtractor;
import roomescape.domain.auth.token.JwtTokenProvider;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthHeaderExtractor authHeaderExtractor;

    public LoginCheckInterceptor(JwtTokenProvider jwtTokenProvider, AuthHeaderExtractor authHeaderExtractor) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authHeaderExtractor = authHeaderExtractor;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!hasLoginRequired(handlerMethod)) {
            return true;
        }

        String accessToken = authHeaderExtractor.extractAccessToken(request);
        Claims claims = jwtTokenProvider.getClaims(accessToken);

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        request.setAttribute("loginMember", new LoginMember(userId, username));

        return true;
    }

    private boolean hasLoginRequired(HandlerMethod handlerMethod) {
        return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), LoginRequired.class) != null
                || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), LoginRequired.class) != null;
    }
}
