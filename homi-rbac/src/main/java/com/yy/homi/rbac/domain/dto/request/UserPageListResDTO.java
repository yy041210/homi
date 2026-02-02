package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户分页查询对象")
public class UserPageListResDTO {
    // 分页参数 (PageHelper 需要)
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;
    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "搜索：用户名", example = "admin")
    private String userName;

    @Schema(description = "搜索：手机号", example = "138")
    private String phonenumber;

    @Schema(description = "搜索：邮箱")
    private String email;

    @Schema(description = "搜索：状态 (0正常 1停用)", example = "0")
    private Integer status;

    @Schema(description = "搜索：开始时间戳", example = "1706400000000")
    private Long beginTime;

    @Schema(description = "搜索：结束时间戳", example = "1706486400000")
    private Long endTime;
}