package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 酒店周边信息分页查询请求参数
 */
@Data
@Schema(description = "酒店周边信息分页查询请求参数")
public class HotelSurroundingPageListReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "酒店ID (精确匹配)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hotelId;

    @Schema(description = "分类 (1-交通 2-景点 3-购物等)")
    private Integer category;

    @Schema(description = "周边名称 (支持模糊搜索，如：东方明珠)")
    private String surroundingName;

    @Schema(description = "排序规则 (默认按seq升序  1: 按距离近到远)")
    private Integer sortRule;
}