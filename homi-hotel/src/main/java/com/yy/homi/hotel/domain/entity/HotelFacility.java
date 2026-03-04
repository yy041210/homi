package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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

    private String tags;  //标签 例如： 收费,每天80

    /**
     * 状态：启用/禁用
     */
    private Integer status;

    private String imageUrl;

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
