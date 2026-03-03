package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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

    private Integer seq;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新者 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;



}
