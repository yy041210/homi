package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

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

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "省份ID")
    private Integer provinceId;

    @Schema(description = "城市ID")
    private Integer cityId;

    @Schema(description = "区县ID")
    private Integer districtId;

    @Schema(description = "搜索：开始时间戳", example = "1706400000000")
    private Date beginTime;

    @Schema(description = "搜索：结束时间戳", example = "1706486400000")
    private Date endTime;

}