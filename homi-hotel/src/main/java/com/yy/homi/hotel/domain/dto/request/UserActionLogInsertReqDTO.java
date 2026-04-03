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

    @Schema(description = "触发行为的房型", example = "1张1.8m大床")
    private String bedType;

    @Schema(description = "当时展示的房型价格或者搜索的价格", example = "3450.0", required = true)
    @NotNull(message = "价格信息不能为空")
    private Double showPrice;

    @Schema(description = "行为类型：VIEW(浏览), FAVORITE(收藏), CLICK_TRIP(跳转)，SEARCH", example = "VIEW", required = true)
    @NotBlank(message = "行为类型不能为空")
    private String actionType;

    @Schema(description = "城市ID", example = "310100")
    private String cityId;

    @Schema(description = "浏览时卫生分数", example = "2")
    private Float hygieneScore;
    @Schema(description = "浏览时设备分数", example = "2")
    private Float deviceScore;
    @Schema(description = "浏览时环境分数", example = "2")
    private Float environmentScore;
    @Schema(description = "浏览时服务分数", example = "2")
    private Float serviceScore;
    @Schema(description = "酒店综合评分", example = "4.8")
    private Float commentScore;
    @Schema(description = "酒店评价数", example = "4.8")
    private Integer commentCount;
    @Schema(description = "浏览时房型的可住人数或者搜索时", example = "2")
    private Integer maxOccupancy;
    @Schema(description = "浏览时房型大小", example = "2")
    private Integer minArea;
    @Schema(description = "浏览时房型大小", example = "2")
    private Integer maxArea;

    @Schema(description = "浏览时酒店坐标", example = "2")
    private String location;

    //搜索特有字段
    @Schema(description = "搜索的酒店设备", example = "2")
    private String hotelFacilities;
    @Schema(description = "搜索的房型设备", example = "2")
    private String roomFacilities;


}