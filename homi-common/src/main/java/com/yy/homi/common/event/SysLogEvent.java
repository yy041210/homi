package com.yy.homi.common.event;

import com.yy.homi.common.domain.entity.SysLog;
import org.springframework.context.ApplicationEvent;

/**
 * 系统日志事件：用于在模块间传递日志数据
 */
public class SysLogEvent extends ApplicationEvent {
    private final SysLog sysLog;

    public SysLogEvent(Object source, SysLog sysLog) {
        super(source);
        this.sysLog = sysLog;
    }

    public SysLog getSysLog() {
        return sysLog;
    }
}