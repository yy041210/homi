package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.List;

//房型基本信息表
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_room")
public class HotelRoom {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String hotelId; //关联的酒店id
    private String name; //房型名字
    private String area;  //面积
    private String floor; //楼层
    private String bedType; //床型
    @TableField("`window`")
    private String window;  // 有窗 | 无窗 | 落地窗 | ...
    private String wifi;  // WIFI免费 | ...
    private String smoke;  // 禁烟 | 可吸烟 | ..
    private Integer maxOccupancy; //最大可住人数
    private String highlightFields; //高亮字段 wifi,window
    private Integer status;


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
