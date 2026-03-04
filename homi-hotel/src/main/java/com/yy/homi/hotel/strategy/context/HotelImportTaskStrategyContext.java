package com.yy.homi.hotel.strategy.context;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.constant.RbacConstants;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//酒店导入策略的上下文对象
@Component
public class HotelImportTaskStrategyContext {

    private static final Map<String, HotelImportTaskStrategy> HOTEL_IMPORT_TASK_STRATEGY_MAP = new ConcurrentHashMap<>();

    @Autowired
    private HotelImportTaskStrategyContext(List<HotelImportTaskStrategy> hotelImportTaskStrategyList) {
        if (CollectionUtil.isNotEmpty(hotelImportTaskStrategyList)) {
            hotelImportTaskStrategyList.stream().forEach(hotelImportTaskStrategy -> {
                HOTEL_IMPORT_TASK_STRATEGY_MAP.put(hotelImportTaskStrategy.getTaskType(), hotelImportTaskStrategy);
            });
        }
    }

    public void execute(String taskId, String filePath, String taskType) {
        HotelImportTaskStrategy hotelImportTaskStrategy = HOTEL_IMPORT_TASK_STRATEGY_MAP.get(taskType);
        if (hotelImportTaskStrategy == null) {
            throw new ServiceException("未找到任务类型为 [" + taskType + "] 的处理策略");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = RbacConstants.ADMIN_ROLE_ID;
        if (principal != null && !principal.equals(RbacConstants.SECURITY_DEFAULT_PRINCIPAL)) {
            userId = principal.toString();
        }

        // 2. 调用策略执行逻辑
        // strategy.execute 方法通常被标注为 @Async 以实现异步处理
        hotelImportTaskStrategy.execute(taskId, filePath, userId);
    }

    public boolean existStrategyType(String taskType){
        if(StrUtil.isEmpty(taskType)){
            return false;
        }
        if (HOTEL_IMPORT_TASK_STRATEGY_MAP.get(taskType) != null) {
            return true;
        }
        return false;
    }

}
