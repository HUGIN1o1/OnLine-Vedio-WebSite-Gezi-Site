package com.gezicoding.geligeli.model.dto.user;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RegisterRequest {
    String account;
    String password;
    String verificationCode;
    String nickname;
}
