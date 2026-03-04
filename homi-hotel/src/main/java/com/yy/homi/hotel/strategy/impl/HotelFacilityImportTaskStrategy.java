package com.yy.homi.hotel.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelFacility;
import com.yy.homi.hotel.domain.entity.HotelFacilityType;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelFacilityTypeMapper;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.service.HotelFacilityService;
import com.yy.homi.hotel.service.HotelFacilityTypeService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Slf4j
@Component
public class HotelFacilityImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelFacilityService hotelFacilityService;

    @Autowired
    private HotelFacilityTypeService hotelFacilityTypeService;

    @Autowired
    private HotelBaseMapper hotelBaseMapper;

    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;
    @Autowired
    private HotelFacilityTypeMapper hotelFacilityTypeMapper;

    @Override
    public String getTaskType() {
        return "HOTEL_FACILITY";
    }

    @Async(value = "homiExecutor")
    @Override
    public void execute(String taskId, String filePath, String userId) {
        log.info("开始执行酒店设备异步导入任务, TaskId: {}, File: {}", taskId, filePath);
        if (StrUtil.isEmpty(taskId) || StrUtil.isEmpty(filePath) || StrUtil.isEmpty(userId)) {
            throw new ServiceException("taskId,filePath或userId不能为空！");
        }
        HotelImportTask hotelImportTask = hotelImportTaskMapper.selectById(taskId);
        if (hotelImportTask == null) {
            throw new ServiceException("任务不存在！");
        }
        File tempFile = new File(filePath);
        if (!tempFile.exists() || !tempFile.isFile()) {
            throw new ServiceException("文件不存在！");
        }

        try {
            hotelImportTaskMapper.updateToRunning(taskId, HotelImportTask.STATUS_RUNNING);  //改为运行中状态

            //加载所有设备类型
            Map<String, String> typeNameIdMap = hotelFacilityTypeMapper.selectList(null).stream().collect(Collectors.toMap(
                    HotelFacilityType::getName,
                    HotelFacilityType::getId
            ));

            // 使用 AtomicInteger 记录总处理数
            AtomicInteger totalProcessed = new AtomicInteger(0);
            // 使用 List 作为缓存
            List<Map<Integer, String>> cachedDataList = new ArrayList<>();
            // 批次大小
            int BATCH_SIZE = 1000;
            EasyExcel.read(filePath, new ReadListener<Map<Integer, String>>() {
                        @Override
                        public void invoke(Map<Integer, String> data, AnalysisContext context) {
                            // 每读取一行数据，加入缓存
                            cachedDataList.add(data);

                            // 达到BATCH_SIZE了，需要去存储一次数据库
                            if (cachedDataList.size() >= BATCH_SIZE) {
                                processData(cachedDataList);
                                // 存储完成清理list
                                cachedDataList.clear();
                            }
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // 这里也要保存数据，确保最后留下的数据也存储到数据库
                            if (!cachedDataList.isEmpty()) {
                                processData(cachedDataList);
                                cachedDataList.clear();
                            }

                            // 更新任务状态为完成
                            hotelImportTaskMapper.updateToSuccess(taskId, HotelImportTask.STATUS_SUCCESS);
                            log.info("酒店设备异步任务执行完成! taskId；{}", taskId);
                        }

                        @Override
                        public void onException(Exception exception, AnalysisContext context) throws Exception {
                            // 更新任务状态为失败
                            hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, "Excel读取异常! 异常信息：" + exception.getMessage());
                            throw exception;
                        }

                        /**
                         * 处理一批数据
                         */
                        private void processData(List<Map<Integer, String>> dataList) {
                            int currentBatchSize = dataList.size();
                            int processed = totalProcessed.addAndGet(currentBatchSize);

                            log.info("处理批次数据，当前批次大小：{}，累计处理：{}", currentBatchSize, processed);

                            Map<String, HotelFacilityType> hotelFacilityTypeNameMap = new HashMap<>();
                            List<HotelFacility> hotelFacilities = new ArrayList<>();
                            Set<String> existHotelIds = new HashSet<>(); //当前批次数据的hotelIds

                            for (Map<Integer, String> rowData : dataList) {
                                String hotelId = rowData.get(0);
                                String parentCategory = rowData.get(1);  //父级分类
                                String facilityName = rowData.get(2);  //设施名称
                                String statusStr = rowData.get(4);
                                String imageUrl = rowData.get(5);
                                String seqStr = rowData.get(6);


                                // 必填字段校验
                                if (StrUtil.isEmpty(hotelId) || StrUtil.isEmpty(parentCategory) ||
                                        StrUtil.isEmpty(facilityName) || StrUtil.isEmpty(statusStr) || StrUtil.isEmpty(seqStr)) {
                                    continue;
                                }

                                String tags = null;
                                if (!StrUtil.isEmpty(rowData.get(3))) {
                                    tags = rowData.get(3).trim().replaceAll(",", "").replaceAll("\\|", ",");
                                }

                                existHotelIds.add(hotelId);

                                // 分类处理 - 修复bug: 应该用parentCategory作为key
                                if (typeNameIdMap.get(parentCategory) == null && hotelFacilityTypeNameMap.get(parentCategory) == null) {
                                    HotelFacilityType hotelFacilityType = new HotelFacilityType();
                                    hotelFacilityType.setName(parentCategory);
                                    hotelFacilityType.setIcon("icon-facility-type");
                                    hotelFacilityType.setSeq(Integer.parseInt(seqStr));
                                    hotelFacilityType.setCreateBy(userId);
                                    hotelFacilityType.setCreateTime(new Date());
                                    hotelFacilityType.setUpdateBy(userId);
                                    hotelFacilityType.setUpdateTime(new Date());
                                    hotelFacilityTypeNameMap.put(parentCategory, hotelFacilityType);
                                }

                                // 设施
                                HotelFacility hotelFacility = new HotelFacility();
                                hotelFacility.setHotelId(hotelId);
                                hotelFacility.setName(facilityName);
                                hotelFacility.setHotelFacilityTypeId(parentCategory);  //先设置为类型名，等会再设置分类id
                                hotelFacility.setIcon("icon-facility");
                                hotelFacility.setTags(tags);
                                hotelFacility.setStatus(Integer.parseInt(statusStr));
                                hotelFacility.setImageUrl(imageUrl);
                                hotelFacility.setSeq(Integer.parseInt(seqStr));
                                hotelFacility.setCreateBy(userId);
                                hotelFacility.setCreateTime(new Date());
                                hotelFacility.setUpdateBy(userId);
                                hotelFacility.setUpdateTime(new Date());
                                hotelFacilities.add(hotelFacility);
                            }

                            // 先保存新分类
                            if (!hotelFacilityTypeNameMap.isEmpty()) {
                                Collection<HotelFacilityType> hotelFacilityTypes = hotelFacilityTypeNameMap.values();
                                hotelFacilityTypeService.saveBatch(hotelFacilityTypes);
                                log.info("保存新分类：{}条", hotelFacilityTypes.size());

                                // 更新分类ID映射
                                Map<String, String> newTypeNameIdMap = hotelFacilityTypes.stream()
                                        .collect(Collectors.toMap(HotelFacilityType::getName, HotelFacilityType::getId));

                                // 查询所有存在的酒店
                                if (!existHotelIds.isEmpty()) {
                                    List<String> existHotelIdsDB = hotelBaseMapper.selectList(
                                            new LambdaQueryWrapper<HotelBase>().in(HotelBase::getId, existHotelIds)
                                    ).stream().map(HotelBase::getId).collect(Collectors.toList());

                                    // 过滤数据库没有酒店的设备，并设置分类ID
                                    List<HotelFacility> validHotelFacilities = hotelFacilities.stream()
                                            .filter(item -> existHotelIdsDB.contains(item.getHotelId()))
                                            .map(item -> {
                                                // 先从新分类映射中找，再从原有映射中找
                                                String hotelFacilityTypeId = newTypeNameIdMap.get(item.getHotelFacilityTypeId());
                                                if (hotelFacilityTypeId == null) {
                                                    hotelFacilityTypeId = typeNameIdMap.get(item.getHotelFacilityTypeId());
                                                }
                                                if (hotelFacilityTypeId != null) {
                                                    item.setHotelFacilityTypeId(hotelFacilityTypeId);
                                                }
                                                return item;
                                            })
                                            .filter(item -> item.getHotelFacilityTypeId() != null) // 过滤掉没有分类ID的
                                            .collect(Collectors.toList());

                                    // 保存设备
                                    if (!validHotelFacilities.isEmpty()) {
                                        hotelFacilityService.saveBatch(validHotelFacilities);
                                        log.info("保存设备：{}条", validHotelFacilities.size());
                                    }
                                }
                            }

                            // 更新导入任务进度
                            hotelImportTaskMapper.incrementProcessedCount(taskId, dataList.size());
                        }

                    })
                    .sheet()
                    .headRowNumber(1) //跳过第一行表头
                    .doRead();
        } finally {
            // 最后删除临时文件
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("清理临时文件: {}, 状态: {}", filePath, deleted);
            }
        }


    }
}
