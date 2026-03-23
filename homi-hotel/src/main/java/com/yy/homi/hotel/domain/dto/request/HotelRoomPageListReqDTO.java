package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "房型分页查询请求参数")
public class HotelRoomPageListReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "酒店ID")
    private String hotelId;

    @Schema(description = "房型名称 (支持模糊搜索，如：观景)")
    private String name;

    // --- 面积查询（建议范围查询） ---
    @Schema(description = "最小面积 (数值，如：25)")
    private Double minArea;

    @Schema(description = "最大面积 (数值，如：40)")
    private Double maxArea;


    @Schema(description = "床型 (如：大床)")
    private String bedType;

    @Schema(description = "最大入住人数", example = "2")
    private Integer maxOccupancy;

    @Schema(description = "状态 (0:正常, 1:下架)")
    private Integer status;

    @Schema(description = "开始时间")
    private Date beginTime;

    @Schema(description = "结束时间")
    private Date endTime;

}