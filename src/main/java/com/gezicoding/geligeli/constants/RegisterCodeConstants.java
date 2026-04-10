package com.gezicoding.geligeli.constants;

public class RegisterCodeConstants {

    /**
     * Redis 中存储注册验证码的 key 前缀。
     */
    public static final String REDIS_REGISTER_CODE_PREFIX = "USER:CODE:";


    /**
     * 注册相关敏感信息加密使用的盐值。
     */
    public static final String SALT = "HUGIN1_1";

    /**
     * 发送验证码线程池大小。
     */
    public static final Integer SEND_MESSAGE_CODE_THREAD_POOL_SIZE = 3;

    /**
     * 邮箱格式校验正则表达式。
     */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /**
     * 13 开头 11 位手机号校验正则表达式。
     */
    public static final String PHONE_REGEX = "^13\\d{9}$";

}
