package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 酒店设施分页查询请求参数
 */
@Data
@Schema(description = "酒店设施分页查询请求参数")
public class HotelFacilityPageListReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "酒店ID (精确匹配)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hotelId;

    @Schema(description = "设施名称 (模糊搜索)")
    private String name;

    @Schema(description = "设施类型ID")
    private String hotelFacilityTypeId;

    @Schema(description = "状态 (0:启用, 1:禁用)")
    private Integer status;

    @Schema(description = "标签关键字 (模糊搜索)")
    private String tags;

    @Schema(description = "排序规则 (1: 按排序值seq升序, 2: 按创建时间倒序)", example = "1")
    private Integer sortRule = 1;
}