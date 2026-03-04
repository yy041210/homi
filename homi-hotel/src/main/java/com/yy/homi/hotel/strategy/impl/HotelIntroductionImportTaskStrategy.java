package com.yy.homi.hotel.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.service.HotelBaseService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HotelIntroductionImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelBaseService hotelBaseService;

    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;

    @Override
    public String getTaskType() {
        return "HOTEL_INTRODUCTION";
    }

    @Async("homiExecutor")
    @Override
    public void execute(String taskId, String filePath, String userId) {
        log.info("开始执行酒店简介相关内容异步导入任务, TaskId: {}, File: {}", taskId, filePath);
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

        hotelImportTaskMapper.updateToRunning(taskId, HotelImportTask.STATUS_RUNNING);  //改为运行中状态

        // 使用 List 作为缓存
        List<Map<Integer, String>> cachedDataList = new ArrayList<>();
        // 批次大小
        int BATCH_SIZE = 1000;

        //查询所有酒店id
        Set<String> allExistHotelIds = hotelBaseService.list().stream().map(HotelBase::getId).collect(Collectors.toSet());

        try {
            EasyExcel.read(tempFile, new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
                    cachedDataList.add(data);  //添加一行数据
                    if (cachedDataList.size() >= BATCH_SIZE) {
                        processData(cachedDataList);  //处理一批数据
                        cachedDataList.clear();
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
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

                void processData(List<Map<Integer, String>> dataList) {

                    List<HotelBase> updateHotelBases = new ArrayList<>();

                    for (Map<Integer, String> rowData : dataList) {
                        String hotelId = rowData.get(0);
                        String openYearStr = rowData.get(1);  //开业年份
                        String roomCountStr = rowData.get(2);  //客房数
                        String phone = rowData.get(3);   //
                        String introduction = rowData.get(4); //简介

                        if (StrUtil.isNotEmpty(hotelId) && allExistHotelIds.contains(hotelId)) {
                            HotelBase hotelBase = new HotelBase();
                            hotelBase.setId(hotelId);
                            hotelBase.setOpenYear(StrUtil.isEmpty(openYearStr) ? null : Integer.parseInt(openYearStr));
                            hotelBase.setRoomCount(StrUtil.isEmpty(roomCountStr) ? null : Integer.parseInt(roomCountStr));
                            hotelBase.setPhone(phone);
                            hotelBase.setDescription(introduction);
                            hotelBase.setUpdateBy(userId);
                            hotelBase.setUpdateTime(new Date());
                            updateHotelBases.add(hotelBase);
                        }

                    }

                    //修改hotelBase
                    hotelBaseService.updateBatchById(updateHotelBases);

                    hotelImportTaskMapper.incrementProcessedCount(taskId,dataList.size());
                }


            }).sheet().headRowNumber(1).doRead();  //不读取第一行
        } finally {
            //删除临时文件
            tempFile.delete();
        }

    }
}
