package com.gezicoding.geligeli.data.SendVertification;

import java.io.Serializable;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SendVertificationCodeResponse implements Serializable {
    int code;
    String message;
    Object data;
}
