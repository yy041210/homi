package com.yy.homi.rbac.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_city")
public class SysCity {

    @TableId(type = IdType.INPUT)
    private Integer id;

    private String name;

    private String nameEn;

    private Integer provinceId;

    private BigDecimal centerLng;

    private BigDecimal centerLat;

    private Integer sort;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // 非数据库字段：该城市下的区县列表
    @TableField(exist = false)
    private List<SysDistrict> districts;
}