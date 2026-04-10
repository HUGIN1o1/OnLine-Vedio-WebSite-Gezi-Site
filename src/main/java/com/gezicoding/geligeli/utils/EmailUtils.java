package com.gezicoding.geligeli.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.stereotype.Component;

import com.gezicoding.geligeli.constants.SMSConstant;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class EmailUtils {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4);

    public static void sendEmailCodeAysc(String address, String code) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                sendEmailCode(address, code);
            } catch (EmailException e) {
                log.error("邮件发送到 {} 的验证码为 {} 的邮件异常", address, code, e);
            }
        });
    }

    

    public static void sendEmailCode(String address, String code) throws EmailException {
        System.setProperty(SMSConstant.SMTP_SSL_PROTOCOLS_KEY, SMSConstant.SMTP_SSL_PROTOCOLS_VALUE);

        SimpleEmail mail = new SimpleEmail();
        mail.setHostName(SMSConstant.SMTP_HOST);        
        // "你的邮箱号"+ "上文开启SMTP获得的授权码"
        mail.setAuthentication(SMSConstant.SMTP_ACCOUNT, SMSConstant.SMTP_AUTH_CODE);
        // 发送邮件 "你的邮箱号"+"发送时用的昵称"
        mail.setFrom(SMSConstant.SMTP_ACCOUNT, SMSConstant.SMTP_FROM_NICKNAME);
        // 使用安全链接
        mail.setSSLOnConnect(true);
        // 接收用户的邮箱
        mail.addTo(address);
        // 邮件的主题(标题)
        mail.setSubject(SMSConstant.EMAIL_SUBJECT);
        // 邮件的内容
        mail.setMsg(String.format(SMSConstant.EMAIL_CONTENT_TEMPLATE, code));
        // 发送
        mail.send();
    }

}
