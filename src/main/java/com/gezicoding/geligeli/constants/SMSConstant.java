package com.gezicoding.geligeli.constants;

public class SMSConstant {

    /**
     * SMTP SSL 协议系统属性 key。
     */
    public static final String SMTP_SSL_PROTOCOLS_KEY = "mail.smtp.ssl.protocols";

    /**
     * SMTP SSL 协议版本。
     */
    public static final String SMTP_SSL_PROTOCOLS_VALUE = "TLSv1.2";

    /**
     * QQ 邮箱 SMTP 主机地址。
     */
    public static final String SMTP_HOST = "smtp.qq.com";

    /**
     * 发件邮箱账号。
     */
    public static final String SMTP_ACCOUNT = "1224242657@qq.com";

    /**
     * 发件邮箱 SMTP 授权码。
     */
    public static final String SMTP_AUTH_CODE = "dugvotcqptajgdba";

    /**
     * 发件显示昵称。
     */
    public static final String SMTP_FROM_NICKNAME = "Geligeli";

    /**
     * 验证码过期时间（分钟）。
     */
    public static final Integer SMS_EXPIRE_TIME = 5;

    /**
     * 验证码邮件主题。
     */
    public static final String EMAIL_SUBJECT = "Geligeli网，注册验证码请查收";

    /**
     * 验证码邮件正文模板。
     */
    public static final String EMAIL_CONTENT_TEMPLATE = "您的验证码为:%s(一分钟内有效)";

    /**
     * 验证码发送成功消息。
     */
    public static final String SMS_SEND_SUCCESS_MSG = "验证码发送成功";
}
