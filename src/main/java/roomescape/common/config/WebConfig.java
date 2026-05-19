package roomescape.common.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.common.auth.LoginRequiredArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginRequiredArgumentResolver loginRequiredArgumentResolver;

    public WebConfig(LoginRequiredArgumentResolver loginRequiredArgumentResolver) {
        this.loginRequiredArgumentResolver = loginRequiredArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(loginRequiredArgumentResolver);
    }
}
