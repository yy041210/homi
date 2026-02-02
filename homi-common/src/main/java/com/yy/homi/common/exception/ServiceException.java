package com.yy.homi.common.exception;

/**
 * 业务异常
 */
public class ServiceException extends RuntimeException {
    private Integer code;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public ServiceException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}