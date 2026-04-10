package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.user.LoginCodeRequest;
import com.gezicoding.geligeli.model.dto.user.LoginPasswordRequest;
import com.gezicoding.geligeli.model.dto.user.RegisterRequest;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.vo.user.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {

    /**
     * 发送验证码
     * @param account
     * @throws Exception
     */
    void sendVerticatinCode(String account) throws Exception;

    /**
     * 异步发送验证码
     * @param account
     */
    void sendVertificationCodeAsync(String account);

    /**
     * 注册
     * @param request
     * @param httpServletRequest
     * @return
     */
    LoginResponse register(RegisterRequest request, HttpServletRequest httpServletRequest);


    /**
     * 登录
     * @param request
     * @param httpServletRequest
     * @return
     */
    LoginResponse loginPassword(LoginPasswordRequest request, HttpServletRequest httpServletRequest);


    /**
     * 验证码登录
     * @param request
     * @param httpServletRequest
     * @return
     */
    LoginResponse loginCode(LoginCodeRequest request, HttpServletRequest httpServletRequest);
}
