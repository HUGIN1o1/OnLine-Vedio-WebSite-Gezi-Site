package com.gezicoding.geligeli.data.register;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RegisterResponse {
    private int code;
    private Object data;
    private String message;

}
