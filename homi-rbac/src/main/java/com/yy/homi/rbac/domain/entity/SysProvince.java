package com.yy.homi.rbac.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 省份表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_province")
public class SysProvince {

    @TableId(type = IdType.INPUT)
    private Integer id;  // 省份ID（高德adcode）

    private String name;  // 省份名称

    private String nameEn;  // 省份英文名

    private BigDecimal centerLng;  // 中心点经度

    private BigDecimal centerLat;  // 中心点纬度

    private Integer sort;  // 排序

    private Integer status;  // 状态 0-禁用 1-启用

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}