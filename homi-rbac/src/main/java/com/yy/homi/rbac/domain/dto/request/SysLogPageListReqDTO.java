package com.yy.homi.rbac.domain.dto.request;

import lombok.Data;

/**
 * 系统日志分页查询请求对象
 */
@Data
public class SysLogPageListReqDTO {
    /** 模块标题 */
    private String title;
    /** 操作类型（0其他，1新增 2删除 3修改 4查询） */
    private Integer operType;
    /** 操作人员 */
    private String operName;
    /** 操作状态（0正常 1异常） */
    private Integer status;
    /** 当前页码 */
    private Integer pageNum = 1;
    /** 每页条数 */
    private Integer pageSize = 10;
}