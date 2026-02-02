package com.yy.homi.common.domain.entity;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * 响应实体类 (使用 Object 接收数据)
 */
@Data
public class R implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;      // 状态码
    private String msg;    // 返回消息
    private Object data;   // 数据对象

    public static R ok() {
        return restResult(null, HttpStatus.OK.value(), "操作成功");
    }

    public static R ok(Object data) {
        return restResult(data,  HttpStatus.OK.value(), "操作成功");
    }

    public static R fail(String msg) {
        return restResult(null,  HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }

    public static R fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    private static R restResult(Object data, int code, String msg) {
        R r = new R();
        r.setCode(code);
        r.setData(data);
        r.setMsg(msg);
        return r;
    }
}