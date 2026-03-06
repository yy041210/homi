package com.yy.homi.rbac.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 区县表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_district")
public class SysDistrict {

    @TableId(type = IdType.INPUT)
    private Integer id;  // 区县ID（高德adcode）

    private String name;  // 区县名称

    private String nameEn;  // 区县英文名

    private Integer cityId;  // 所属城市ID

    private BigDecimal centerLng;  // 中心点经度

    private BigDecimal centerLat;  // 中心点纬度

    private Integer sort;  // 排序

    private Integer status;  // 状态 0-禁用 1-启用

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}