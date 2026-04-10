package com.gezicoding.geligeli.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gezicoding.geligeli.utils.DeviceUtil;
import com.gezicoding.geligeli.utils.JWTUtils;

import io.jsonwebtoken.Claims;


@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token != null && !token.isEmpty()) {
            Claims claims = JWTUtils.parse(token);
            if (claims != null) {
                String userId = claims.getSubject();
                String requestDevice = DeviceUtil.getHttpRequestDevice(request);
                String redisKey = redisTemplate.opsForValue().get(requestDevice + ":" + userId);
                boolean isLogin = redisKey != null && !redisKey.isEmpty();

                if (isLogin) {
                    return true;
                } else {
                    refuseUnauthorized(response);
                    return false;
                }
            }
        }
        refuseUnauthorized(response);
        return false;
    }


    private void refuseUnauthorized(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"code\": 401, \"message\": \"未授权的访问，请提供有效的Token\"}");
    }
}
