package com.gezicoding.geligeli.data.SendVertification;

import java.io.Serializable;

import lombok.Data;

@Data
public class SendVertificationCodeRequest implements Serializable {
    String account;
}
