package com.mentis.hrms.config;

import com.mentis.hrms.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/dashboard/**", "/candidate/dashboard/**")
                .excludePathPatterns(
                        "/candidate/auth/**",
                        "/static/**",
                        "/candidate/login",           // ADD THIS
                        "/candidate/create-password",
                        "/candidate/forgot-password",
                        "/candidate/reset-password/**"
                );
    }
}