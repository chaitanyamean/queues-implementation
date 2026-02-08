package com.url_shortner.project.config;

import com.url_shortner.project.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    // private final RateLimitInterceptor rateLimitInterceptor;

    // Interceptor removed as it is converted to a Filter
    // @Override
    // public void addInterceptors(InterceptorRegistry registry) {
    // registry.addInterceptor(rateLimitInterceptor)
    // .addPathPatterns("/shorten", "/redirect");
    // }
}
