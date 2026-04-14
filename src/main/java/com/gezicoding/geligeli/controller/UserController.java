package com.gezicoding.geligeli.controller;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.Result;
import com.gezicoding.geligeli.common.ResultUtils;
import com.gezicoding.geligeli.constants.SMSConstant;
import com.gezicoding.geligeli.model.dto.user.LoginCodeRequest;
import com.gezicoding.geligeli.model.dto.user.LoginPasswordRequest;
import com.gezicoding.geligeli.model.dto.user.RegisterRequest;
import com.gezicoding.geligeli.model.dto.user.FollowRequest;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.vo.user.LoginResponse;
import com.gezicoding.geligeli.model.vo.user.UserListResponse;
import com.gezicoding.geligeli.service.FollowService;
import com.gezicoding.geligeli.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FollowService followService;

    /**
     * 根据 userId 查询用户（路径与 /list 区分，避免 list 被当成 id）
     */
    @GetMapping("/{userId}")
    public Result<User> getByUserId(@PathVariable("userId") Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ValidError("用户不存在");
        }
        return Result.OK(user);
    }

    /**
     * 发送验证码
     * @param account
     * @return BaseResponse<String>
     */
    @GetMapping("/sendVerificationCode")
    public BaseResponse<String> sendVerticatinCode(@RequestParam("account") String account) {
        userService.sendVertificationCodeAsync(account);
        return ResultUtils.success(SMSConstant.SMS_SEND_SUCCESS_MSG);
    }


    /**
     * 注册
     * @param request
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<LoginResponse> register(@RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        LoginResponse loginResponse = userService.register(request, httpServletRequest);
        return ResultUtils.success(loginResponse);
    }


    /**
     * 密码登录
     * @param loginPasswordRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/loginPassword")
    public BaseResponse<LoginResponse> loginPassword(@Valid @RequestBody LoginPasswordRequest loginPasswordRequest, HttpServletRequest request) {
        return ResultUtils.success(userService.loginPassword(loginPasswordRequest, request));
    }


    /**
     * 验证码登录
     * @param loginCodeRequest
     * @param request
     * @return
     */
    @PostMapping("/loginCode")
    public BaseResponse<LoginResponse> loginCode(@Valid @RequestBody LoginCodeRequest loginCodeRequest, HttpServletRequest request) {
        return ResultUtils.success(userService.loginCode(loginCodeRequest, request));
    }


    @PostMapping("/follow")
    public BaseResponse<Boolean> follow(@Valid @RequestBody FollowRequest followRequest) {
        return ResultUtils.success(followService.follow(followRequest));
    }


    @GetMapping("/following/list")
    public BaseResponse<List<UserListResponse>> followingList(@Valid @NotEmpty(message = "用户id不能为空") @RequestParam Long userId) {
        return ResultUtils.success(followService.followList(userId));
    }


    @GetMapping("/followers/list")
    public BaseResponse<List<UserListResponse>> followersList(@Valid @NotEmpty(message = "用户id不能为空") @RequestParam Long userId) {
        return ResultUtils.success(followService.followerList(userId));
    }


}