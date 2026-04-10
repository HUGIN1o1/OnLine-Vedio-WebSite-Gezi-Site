package com.gezicoding.geligeli.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户验证码登录 DTO
 */
@Data
@Accessors(chain = true)
public class LoginCodeRequest {


    /** 
     * 手机号
     */
    @NotBlank(message = "手机号/邮箱不能为空")
    private String account;


    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
