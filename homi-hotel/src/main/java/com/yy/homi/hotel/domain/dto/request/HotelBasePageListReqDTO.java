package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 酒店基础信息分页查询请求对象
 */
@Data
@Schema(description = "管理端-酒店分页查询请求")
public class HotelBasePageListReqDTO {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "酒店名称（支持模糊搜索）")
    private String name;

    @Schema(description = "酒店星级")
    private Integer star;

    @Schema(description = "省份ID")
    private Integer provinceId;

    @Schema(description = "城市ID")
    private Integer cityId;

    @Schema(description = "区县ID")
    private Integer districtId;

    @Schema(description = "排序字段 (如: create_time, open_year)")
    private String orderByColumn;

    @Schema(description = "排序方式 (ascending / descending)")
    private boolean isAsc;
}