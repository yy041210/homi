package com.yy.homi.common.aspect;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.constant.RabbitMqConstants; // 引用你定义的常量
import com.yy.homi.common.domain.entity.SysLog;
import com.yy.homi.common.utils.ServletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // 换成 RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Aspect
@Component
@Slf4j
public class LogAspect {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    //环绕通知
    @Around("@annotation(autoLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, AutoLog autoLog) throws Throwable {
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        Object result = null;
        Exception exception = null;

        try {
            // 执行业务方法
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 计算耗时并处理日志
            long takeUpTime = System.currentTimeMillis() - startTime;
            handleLog(joinPoint, autoLog, exception, result, takeUpTime);
        }
    }

    protected void handleLog(final ProceedingJoinPoint joinPoint, AutoLog autoLog,
                             final Exception e, Object jsonResult, long takeUpTime) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            // 封装 SysLog 对象
            SysLog sysLog = new SysLog();

            // 填充用户信息 (Security)
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if(StrUtil.isNotBlank(principal.toString())){
                sysLog.setOperUserId(principal.toString());
            }

            // 填充基础信息
            sysLog.setOperTime(new Date());
            sysLog.setTakeUpTime(takeUpTime);
            sysLog.setOperIp(ServletUtils.getIpAddr(request));
            sysLog.setOperUrl(request.getRequestURI());
            sysLog.setRequestMethod(request.getMethod());

            // 状态判断
            sysLog.setStatus(e == null ? CommonConstants.STATUS_ENABLED : CommonConstants.STATUS_DISABLED);

            // 注解信息
            sysLog.setTitle(autoLog.title());
            sysLog.setBusinessType(autoLog.businessType().ordinal());

            // 方法信息
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            sysLog.setMethod(className + "." + methodName + "()");

            // 参数与结果 (JSON)
            sysLog.setOperParam(ServletUtils.argsArrayToString(joinPoint.getArgs()));
            if (jsonResult != null) {
                sysLog.setJsonResult(JSON.toJSONString(jsonResult));
            }
            if (e != null) {
                sysLog.setErrorMsg(e.getMessage());
            }

            // 推送到 RabbitMQ 交换机
            log.info(">>>> 正在推送到 RabbitMQ: [{}]", sysLog.getTitle());
            rabbitTemplate.convertAndSend(
                    RabbitMqConstants.HOMI_LOG_EXCHANGE,
                    RabbitMqConstants.HOMI_LOG_ROUTING_KEY,
                    sysLog
            );

        } catch (Exception exp) {
            log.error(">>>> 日志 MQ 推送异常: {}", exp.getMessage());
        }
    }
}