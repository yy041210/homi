package com.yy.homi.common.aspect;

import com.alibaba.fastjson.JSON;
import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.domain.entity.SysLog;
import com.yy.homi.common.event.SysLogEvent;
import com.yy.homi.common.utils.ServletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LogAspect {

    // Spring 事件发布器，用于推送 SysLogEvent
    private final ApplicationEventPublisher eventPublisher;

    // 线程本地变量，用于记录起始时间计算耗时
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new ThreadLocal<>();

    /**
     * 定义切点：拦截所有加了 @AutoLog 注解的方法
     */
    @Pointcut("@annotation(com.yy.homi.common.annotation.AutoLog)")
    public void logPointCut() {
    }

    /**
     * 前置通知：记录方法执行开始时间
     */
    @Before("logPointCut()")
    public void doBefore() {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    /**
     * 返回通知：方法正常执行完毕后执行
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    /**
     * 异常通知：方法抛出异常时执行
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    /**
     * 核心逻辑：填充数据并发布事件
     */
    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            // 1. 获取当前 HTTP 请求对象
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            // 2. 获取方法上的 @AutoLog 注解信息
            AutoLog autoLog = ServletUtils.getAnnotation(joinPoint, AutoLog.class);
            if (autoLog == null) return;

            // 3. 封装SysLog 对象
            SysLog sysLog = new SysLog();

            //用户id和用户名
            SecurityContext context = SecurityContextHolder.getContext();
            String userId = context.getAuthentication().getPrincipal().toString();
            sysLog.setOperUserId(userId);

            // 基础信息
            sysLog.setOperTime(new Date());
            sysLog.setOperIp(ServletUtils.getIpAddr(request));
            sysLog.setOperUrl(request.getRequestURI());
            sysLog.setRequestMethod(request.getMethod());
            sysLog.setStatus(e == null ? CommonConstants.STATUS_ENABLED : CommonConstants.STATUS_DISABLED); // 0-正常, 1-异常

            // 注解配置信息
            sysLog.setTitle(autoLog.title());
            sysLog.setBusinessType(autoLog.businessType().ordinal());

            // 方法反射信息
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            sysLog.setMethod(className + "." + methodName + "()");

            // 请求参数处理 (过滤掉敏感对象如 MultipartFile)
            sysLog.setOperParam(ServletUtils.argsArrayToString(joinPoint.getArgs()));

            // 返回值处理
            if (jsonResult != null) {
                sysLog.setJsonResult(JSON.toJSONString(jsonResult));
            }

            // 异常信息处理
            if (e != null) {
                sysLog.setErrorMsg(e.getMessage());
            }

            // 计算耗时
            Long startTime = TIME_THREADLOCAL.get();
            if (startTime != null) {
                sysLog.setTakeUpTime(System.currentTimeMillis() - startTime);
            }

            // 获取当前操作人 (此处需结合你的权限框架，如 Shiro/Security)
            // sysLog.setOperName(SecurityUtils.getUsername());

            // 4. 【核心推送】发布 Spring 事件
            log.info(">>>> 正在推送日志事件: [{}]", sysLog.getTitle());
            eventPublisher.publishEvent(new SysLogEvent(this, sysLog));

        } catch (Exception exp) {
            log.error(">>>> 日志切面处理异常: {}", exp.getMessage());
        } finally {
            TIME_THREADLOCAL.remove(); // 必须清理，防止内存泄漏
        }
    }
}