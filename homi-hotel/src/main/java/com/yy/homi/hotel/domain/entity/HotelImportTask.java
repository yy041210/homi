package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Date;

/**
 * 酒店数据导入任务实体
 * 用于支撑 3w+ 数据异步导入时的进度查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_import_task")
public class HotelImportTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID (使用雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 任务名称 (例如: 濮阳酒店图集导入) */
    private String taskName;

    /** 任务类型 (策略标识: HOTEL_ALBUM, HOTEL_INFO, HOTEL_FACILITY) */
    private String taskType;

    /** 任务状态 (0:排队中, 1:执行中, 2:已完成, 3:执行失败) */
    private Integer status;

    /** 任务总行数 */
    private Integer totalCount;

    /** 当前已写入数据库的行数 */
    private Integer processedCount;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date finishTime;

    /** 耗时(秒) - 可用于前端展示 */
    private Long costTime;

    /**  错误信息 **/
    private String errorMsg;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // --- 状态常量定义 ---
    public static final int STATUS_WAITING = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
}