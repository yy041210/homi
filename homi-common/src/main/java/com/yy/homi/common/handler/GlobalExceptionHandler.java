package com.yy.homi.common.handler;


import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public R handleServiceException(ServiceException e, HttpServletRequest request) {
        log.error("请求地址'{}', 业务异常: {}", request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }


    /**
     * 统一处理所有参数校验/绑定异常
     * 覆盖范围：
     * 1. @RequestBody 校验异常 (MethodArgumentNotValidException)  json请求体解析异常
     * 2. @RequestParam/@PathVariable 校验异常 (ConstraintViolationException) 请求参数解析异常（@NoBlank）
     * 3. 对象绑定/表单校验异常 (BindException) 校验失败（例如数据长度不对@Size），类型转换错误会抛出该异常
     * 3.@RequestBody 校验异常 MissingServletRequestParameterException.class 请求参数不存在会抛出该异常 @RequestParam("id")，但前端没传这个参数
     * 4.处理 JSON 格式错误异常 HttpMessageNotReadableException (比如 JSON 少了引号、括号不匹配等)
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            BindException.class,
            HttpMessageNotReadableException.class
    })
    public R handleValidationException(Exception e, HttpServletRequest request) {
        String msg = "参数校验失败";
        if (e instanceof MissingServletRequestParameterException) {
            log.error("缺少必要的请求参数: {}", e.getMessage());
            msg = String.format("缺少必要的请求参数: %s", ((MissingServletRequestParameterException) e).getParameterName()); // e.getParameterName() 可以拿到具体的参数名，比如 "id"
        } else if (e instanceof MethodArgumentNotValidException) {
            log.error("请求地址'{}',参数校验异常类型: {}, 异常信息: {}", request.getRequestURI(), e.getClass().getName(), e.getMessage());
            // 处理 @RequestBody 对象的校验
            msg = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof ConstraintViolationException) {
            log.error("请求地址'{}',参数校验异常类型: {}, 异常信息: {}", request.getRequestURI(), e.getClass().getName(), e.getMessage());
            // 处理单个参数 (@RequestParam) 的校验
            msg = ((ConstraintViolationException) e).getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
        } else if (e instanceof BindException) {
            log.error("请求地址'{}',参数校验异常类型: {}, 异常信息: {}", request.getRequestURI(), e.getClass().getName(), e.getMessage());
            // 处理表单或 Query 参数绑定到对象的校验
            msg = ((BindException) e).getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof HttpMessageNotReadableException) {
            log.error("请求地址'{}',请求体解析异常: ", request.getRequestURI(),e);
            msg = "请求参数格式错误，请检查 JSON 结构";
        }
        return R.fail(msg);
    }

    /**
     * 处理未知运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("请求地址'{}', 发生未知异常: ", request.getRequestURI(), e);
        return R.fail("服务器运行异常，请联系管理员");
    }

    /**
     * 处理系统级兜底异常
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e, HttpServletRequest request) {
        log.error("请求地址'{}', 系统异常: ", request.getRequestURI(), e);
        return R.fail("系统繁忙，请稍后再试");
    }
}