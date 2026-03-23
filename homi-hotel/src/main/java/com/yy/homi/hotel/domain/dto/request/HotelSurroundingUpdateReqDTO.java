package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;

@Data
@Schema(name = "HotelSurroundingUpdateReqDTO", description = "酒店周边信息更新请求对象")
public class HotelSurroundingUpdateReqDTO {

    @Schema(description = "主键ID", example = "2029073795716759554", required = true)
    @NotBlank(message = "更新记录的ID不能为空")
    private String id;

    @Schema(description = "所属酒店ID", example = "1001")
    private String hotelId;

    @Schema(description = "分类：1-交通 2-景点 3-购物", example = "1")
    private Integer category;

    @Schema(description = "周边地标名称", example = "虹桥火车站")
    private String surroundingName;

    @Schema(description = "距离（单位：米）", example = "500.0")
    private Double distance;

    @Schema(description = "距离描述文字", example = "步行约5分钟")
    private String distanceDesc;

    @Schema(description = "出行方式 (DRIVE-驾车, WALKING-步行, LINEAR-直线距离)", example = "WALKING")
    private String arrivalType;

    @Schema(description = "分类标签 (地铁站/火车站/机场/商圈)", example = "地铁站")
    private String tagName;

    @Schema(description = "纬度", example = "31.2304")
    private Double lat;

    @Schema(description = "经度", example = "121.4737")
    private Double lon;

    @Schema(description = "排序序号 (数值越小越靠前)", example = "1")
    @Min(value = 0, message = "排序序号不能小于0")
    private Integer seq;
}