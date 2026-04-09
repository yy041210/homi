package com.yy.homi.hotel.domain.dto.request;

import lombok.Data;

@Data
public class SparkTaskPageListReqDTO {
    private Integer pageNum = 1;  // 当前页码
    private Integer pageSize = 10; // 每页条数
    
    // 查询条件字段
    private String taskType;   // USER_PROFILING, MODEL_TRAINING
    private Integer status;    // 0-运行中，1-成功，2-失败
}