package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "角色分页查询请求对象")
public class RolePageListReqDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页显示数量", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色权限字符串")
    private String roleKey;

    @Schema(description = "角色状态 (0正常 1停用)")
    private Integer status;

    @Schema(description = "开始时间戳")
    private Date beginTime;

    @Schema(description = "结束时间 (yyyy-MM-dd)")
    private Date endTime;
}