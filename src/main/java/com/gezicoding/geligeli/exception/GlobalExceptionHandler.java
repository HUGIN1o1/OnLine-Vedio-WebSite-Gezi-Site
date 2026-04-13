package com.gezicoding.geligeli.exception;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.common.ResultUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<String> handleEmailException(EmailException e) {
        log.error("邮件发送异常", e.getMessage());
        return ResultUtils.error( HttpStatus.INTERNAL_SERVER_ERROR.value(),  "邮件发送异常");
    }


    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<String> handleBusinessException(BusinessException e) {
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        if (e instanceof BusinessException) {
            throw e;
        }
        log.error("未捕获的运行时异常", e);
        String hint = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误: " + hint);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        String message = errors.toString();
        return ResultUtils.error(ErrorCode.INVALID_PARAMETER_ERROR, message);
    }


}
