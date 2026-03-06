package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "地区新增请求对象")
public class RegionInsertReqDTO {

    @Schema(description = "新增类型")
    @NotNull
    private Integer type;  // 1 - 省 2 - 市  3 - 区

    @Schema(description = "地区编号")
    @NotNull
    private Integer id;

    @Schema(description = "地区名字")
    @NotBlank
    private String name;

    @Schema(description = "地区名 英文")
    private String nameEn;

    @Schema(description = "父级编号")
    private Integer parentId;

    @Schema(description = "启用/禁用")
    private Integer status = 1;

    @Schema(description = "排序字段")
    private Integer sort = 0;

    @Schema(description = "中心点经度")
    private BigDecimal centerLng;  // 中心点经度

    @Schema(description = "中心点纬度")
    private BigDecimal centerLat;  // 中心点纬度

}
