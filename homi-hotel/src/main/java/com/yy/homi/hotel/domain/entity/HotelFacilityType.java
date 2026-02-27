package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//酒店设施类型实体类
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_facility_type")
public class HotelFacilityType {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String name;  //设施类型名称(如 "基础设施")

    private String icon; //该类型对应的icon (如 "icon-basic")

}
