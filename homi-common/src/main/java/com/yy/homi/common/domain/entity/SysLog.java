package com.yy.homi.common.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统操作日志表 sys_log
 */
@Data
@Accessors(chain = true) // 开启链式调用，方便构建对象
@TableName("sys_log")
public class SysLog implements Serializable {

    public static final int OTHER_TYPE = 0;
    public static final int INSERT_TYPE = 1;
    public static final int DELETE_TYPE = 2;
    public static final int UPDATE_TYPE = 3;
    public static final int SELECT_TYPE = 4;

    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模块标题
     */
    private String title;

    /**
     * 业务类型 (0其它 1新增 2修改 3删除 ...)
     */
    private Integer businessType;

    /**
     * 方法名称
     */
    private String method;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 操作人员userId
     */
    private String operUserId;

    /**
     * 操作人员
     */
    private String operName;

    /**
     * 请求URL
     */
    private String operUrl;

    /**
     * 主机地址
     */
    private String operIp;

    /**
     * 请求参数
     */
    private String operParam;

    /**
     * 返回参数
     */
    private String jsonResult;

    /**
     * 操作状态（0正常 1异常）
     */
    private Integer status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 操作时间
     */
    private Date operTime;

    /**
     * 消耗时间 (毫秒)
     */
    private Long takeUpTime;
}