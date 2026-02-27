package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;


//酒店基本信息表
@Data
@TableName("hotel_base")
@NoArgsConstructor
@AllArgsConstructor
public class HotelBase {

    @TableId(type= IdType.ASSIGN_ID)
    private String id;  //主键id
    private String name;  //酒店名
    private String nameEn; //酒店英文名
    private Integer star; //酒店星级
    private Integer openYear;    // 对应：开业年份
    private Integer roomCount;   // 对应：客房数
    private String phone;        // 对应：电话
    private String description;  // 对应：简介内容
    private Integer provinceId; //关联省id
    private Integer cityId; //关联市id
    private String address; //地址
    private Double lat; //纬度
    private Double lon; //经度


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
