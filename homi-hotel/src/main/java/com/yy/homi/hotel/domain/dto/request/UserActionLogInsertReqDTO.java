package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Schema(name = "UserActionLogInsertReqDTO", description = "用户行为日志新增请求对象")
public class UserActionLogInsertReqDTO implements Serializable {

    @Schema(description = "用户id", example = "101", required = true)
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @Schema(description = "酒店ID", example = "101", required = true)
    @NotBlank(message = "酒店ID不能为空")
    private String hotelId;

    @Schema(description = "酒店名称", example = "上海苏宁宝丽嘉酒店")
    private String hotelName;

    @Schema(description = "酒店星级", example = "5")
    private Integer star;

    @Schema(description = "触发行为的房型ID", example = "room_99")
    private String roomId;

    @Schema(description = "触发行为的房型名称", example = "豪华大床房")
    private String roomName;

    @Schema(description = "当时展示的房型价格", example = "3450.0", required = true)
    @NotNull(message = "价格信息不能为空")
    private Double showPrice;

    @Schema(description = "行为类型：VIEW(浏览), FAVORITE(收藏), CLICK_TRIP(跳转)", example = "VIEW", required = true)
    @NotBlank(message = "行为类型不能为空")
    private String actionType;

    @Schema(description = "城市ID", example = "310100")
    private String cityId;

    @Schema(description = "城市名称", example = "上海")
    private String cityName;

    @Schema(description = "酒店综合评分", example = "4.8")
    private Double commentScore;

    @Schema(description = "酒店标签(多个逗号隔开)", example = "停车场,健身房,双早")
    private String hotelTags;
}