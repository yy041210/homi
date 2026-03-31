package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * Spark 任务执行记录实体
 */
@Data
@TableName("spark_task")
public class SparkTask {

    @TableId(type = IdType.AUTO)
    private String id;

    public static final Integer TASK_RUNNING = 0;
    public static final Integer TASK_SUCCESS = 1;
    public static final Integer TASK_ERROR = 2;


    public static final String  USER_PROFILING_TASK = "USER_PROFILING";
    public static final String  MODEL_TRAINING_TASK = "MODEL_TRAINING";


    /**
     * 任务类型：IMAGE_PROFILING, MODEL_TRAINING
     */
    private String taskType;

    /**
     * 任务名称（如：每日用户画像计算、ALS模型训练）
     */
    private String taskName;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 耗时（秒）
     */
    private Long duration;

    /**
     * 任务状态：0-运行中，1-成功，2-失败
     */
    private Integer status;

    /**
     * 本次处理的数据条数
     */
    private Integer processedRecords;

    /**
     * 异常堆栈信息（仅失败时有值）
     */
    private String errorMsg;

    /**
     * 模型存储路径（如果是训练任务，记录保存的位置）
     */
    private String modelPath;


}