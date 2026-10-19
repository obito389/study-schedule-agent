package com.studyplanner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置：注册拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserInterceptor currentUserInterceptor;
    private final com.studyplanner.team.LegacyWriteGuard legacyGuard;

    public WebConfig(CurrentUserInterceptor currentUserInterceptor,com.studyplanner.team.LegacyWriteGuard legacyGuard) {
        this.currentUserInterceptor = currentUserInterceptor;
        this.legacyGuard=legacyGuard;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(currentUserInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(legacyGuard).addPathPatterns("/api/**");
    }
}
