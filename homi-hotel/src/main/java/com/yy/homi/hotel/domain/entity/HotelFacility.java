package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//酒店设施实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_facility")
public class HotelFacility {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String hotelId; //关联的酒店id

    private String hotelFacilityTypeId; //关联的设备类型 id

    /**
     * 设施名称：如 "停车场", "智能马桶", "健身房"
     */
    private String name;

    private String icon; //该类型对应的icon (如 "icon-basic")

    private Integer chargeStandard; //收费标准 0 位置 1标准 2免费 3收费

    private String chargeStandardDesc; //收费标准描述

    /**
     * 状态：启用/禁用
     */
    private Integer status;

    /**
     * 备注：如 "停车场位于酒店后院" 或 "早餐收费 38/人"
     */
    private String remark;
}
