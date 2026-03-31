package com.yy.homi.hotel.controller;

import com.yy.homi.hotel.task.ScheduledTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskTestController {
    @Autowired
    private ScheduledTask scheduledTask;

    @GetMapping("/run")
    public String triggerTask() {
        new Thread(() -> scheduledTask.trainRecommendAlsTask()).start(); // 异步触发，防止前端超时
        return "任务已在后台启动，请查看侧边栏日志";
    }

    @GetMapping("/runUserImage")
    public String runUserImage() {
        new Thread(() -> scheduledTask.processUserProfile()).start(); // 异步触发，防止前端超时
        return "任务已在后台启动，请查看侧边栏日志";
    }
}