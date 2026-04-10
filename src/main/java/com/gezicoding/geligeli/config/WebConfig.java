package com.gezicoding.geligeli.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gezicoding.geligeli.interceptor.JwtInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer{

    @Autowired
    private JwtInterceptor jwtInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/user/sendVerificationCode", "/api/user/register", "/api/user/info",
                        "/api/user/loginCode", "/api/user/getCode","/api/user/focus/list",
                        "/api/user/fans/list", "/api/user/loginPassword", "/api/video/list",
                        "/api/video/detail", "/api/video/comment/list", "/api/video/submit/list",
                        "/api/video/coin/list", "/api/video/like/list", "/api/video/favorite/list",
                        "/api/category", "/api/category/list");
    }
}
