package com.yy.homi.hotel.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;

/**
 * 酒店导入任务分页查询请求参数
 */
@Data
@Schema(description = "酒店导入任务分页查询请求对象")
public class HotelImportTaskPageListReqDTO {

    @Schema(description = "当前页码", defaultValue = "1", example = "1")
    private Integer pageNo = 1;

    @Schema(description = "每页条数", defaultValue = "10", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "任务名称 (支持模糊搜索)", example = "2026年3月酒店增量导入")
    private String taskName;

    @Schema(description = "任务类型", allowableValues = {"HOTEL_ALBUM", "HOTEL_INFO", "HOTEL_FACILITY"})
    private String taskType;

    @Schema(description = "任务状态 (0:排队中, 1:执行中, 2:已完成, 3:执行失败)", allowableValues = {"0", "1", "2", "3"})
    private Integer status;

    @Schema(description = "创建起始时间", example = "2026-03-01 00:00:00")
    private Date beginTime;

    @Schema(description = "创建截止时间", example = "2026-03-31 23:59:59")
    private Date endTime;

    @Schema(description = "创建人用户ID")
    private String createBy;
}