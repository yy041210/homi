package com.yy.homi.hotel.strategy;

import org.springframework.scheduling.annotation.Async;

//酒店相关信息，数据导入任务的抽象策略接口
//实现：酒店基本信息导入任务，酒店图集信息导入任务，酒店设备信息导入任务...
public interface HotelImportTaskStrategy {

    String getTaskType();   //任务类型：HOTEL_ALBUM,HOTEL_BASE,HOTEL_FACILITY

    /**
     * 执行导入逻辑
     * @param taskId 任务ID
     * @param filePath CSV文件路径（或其它参数）
     * @param userId 操作人ID
     */
    void execute(String taskId, String filePath, String userId);
}
