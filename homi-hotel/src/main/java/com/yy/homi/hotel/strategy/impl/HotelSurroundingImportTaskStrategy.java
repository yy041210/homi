package com.yy.homi.hotel.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yy.homi.common.enums.hotel.SurroundingCategoryEnum;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import com.yy.homi.hotel.domain.entity.HotelSurrounding;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.mapper.HotelSurroundingMapper;
import com.yy.homi.hotel.service.HotelSurroundingService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//酒店周边导入任务策略
@Slf4j
@Component
public class HotelSurroundingImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelSurroundingService hotelSurroundingService;

    @Autowired
    private HotelSurroundingMapper hotelSurroundingMapper;
    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;

    @Override
    public String getTaskType() {
        return "HOTEL_SURROUNDING";
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

            // 使用 List 作为缓存
            List<Map<Integer, String>> cachedDataList = new ArrayList<>();
            // 批次大小
            int BATCH_SIZE = 100;

            //所有酒店id
            Set<String> allHotelIds = hotelBaseMapper.selectList(null).stream().map(HotelBase::getId).collect(Collectors.toSet());
            AtomicInteger seq = new AtomicInteger(1);

            EasyExcel.read(tempFile, new ReadListener<Map<Integer, String>>() {

                        @Override
                        public void onException(Exception exception, AnalysisContext context) throws Exception {
                            // 更新任务状态为失败
                            hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, "Excel读取异常! 异常信息：" + exception.getMessage());
                            throw exception;
                        }

                        @Override
                        public void invoke(Map<Integer, String> rowData, AnalysisContext analysisContext) {
                            cachedDataList.add(rowData);  //添加一行数据
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
                            log.info("酒店周边信息异步任务执行完成! taskId；{}", taskId);
                        }

                        void processData(List<Map<Integer, String>> dataList) {
                            List<HotelSurrounding> saveHotelSurroundings = new ArrayList<>();  //需要保存的酒店周边集合

                            Set<String> hotelIds = new HashSet<>();
                            for (Map<Integer, String> rowData : dataList) {
                                String hotelId = rowData.get(0);
                                String category = rowData.get(1);
                                String name = rowData.get(2);
                                String distanceStr = rowData.get(3);
                                String distanceDescStr = rowData.get(4);
                                String travelType = rowData.get(5);
                                String tagName = rowData.get(6);
                                String lnglatStr = rowData.get(7);

                                //数据校验
                                if (StrUtil.isEmpty(hotelId) || StrUtil.isEmpty(name)) {
                                    continue;
                                }

                                //hotelId不存在的直接跳过
                                if (!allHotelIds.contains(hotelId)) {
                                    continue;
                                } else {
                                    hotelIds.add(hotelId);
                                }

                                HotelSurrounding hotelSurrounding = new HotelSurrounding();
                                hotelSurrounding.setHotelId(hotelId);
                                hotelSurrounding.setCategory(SurroundingCategoryEnum.fromDesc(category).getCode());
                                hotelSurrounding.setSurroundingName(name);
                                if (StrUtil.isNotBlank(distanceStr)) {
                                    if (!distanceStr.equals("<100米")) {
                                        if (distanceStr.contains("千米") || distanceStr.equals("公里") || distanceStr.equals("km")) {
                                            distanceStr.replaceAll("千米", "");
                                            distanceStr.replaceAll("公里", "");
                                            distanceStr.replaceAll("km", "");
                                            Double distance = Double.valueOf(distanceStr);
                                            hotelSurrounding.setDistance(distance * 1000);
                                        } else if (distanceStr.contains("米") || distanceStr.equals("m")) {
                                            distanceStr.replaceAll("米", "");
                                            distanceStr.replaceAll("m", "");
                                            Double distance = Double.valueOf(distanceStr);
                                            hotelSurrounding.setDistance(distance);
                                        }

                                    }
                                }
                                hotelSurrounding.setDistanceDesc(distanceDescStr);
                                hotelSurrounding.setArrivalType(travelType);
                                hotelSurrounding.setTagName(tagName);
                                hotelSurrounding.setSeq(seq.incrementAndGet());
                                if (StrUtil.isNotBlank(lnglatStr)) {
                                    String lngStr = lnglatStr.split(",")[0];
                                    String latStr = lnglatStr.split(",")[1];
                                    if (StrUtil.isNotBlank(lngStr)) {
                                        hotelSurrounding.setLon(Double.valueOf(lngStr));
                                    }
                                    if (StrUtil.isNotBlank(latStr)) {
                                        hotelSurrounding.setLat(Double.valueOf(latStr));
                                    }
                                }

                                saveHotelSurroundings.add(hotelSurrounding);
                            }

                            if (CollectionUtil.isNotEmpty(saveHotelSurroundings)) {

                                //根据当前批次数据的hotelIds查询数据库已经存在的 hotelId:category:周边名字的set集合
                                Set<String> existSurroundingSet = hotelSurroundingMapper.selectList(
                                        new LambdaQueryWrapper<HotelSurrounding>()
                                                .in(HotelSurrounding::getHotelId, hotelIds)
                                ).stream().map(item -> item.getHotelId() + ":" + item.getCategory() + ":" + item.getSurroundingName()).collect(Collectors.toSet());

                                //过滤数据
                                saveHotelSurroundings = saveHotelSurroundings.stream().filter(item -> !existSurroundingSet.contains(item.getHotelId() + ":" + item.getCategory() + ":" + item.getSurroundingName())).collect(Collectors.toList());

                                hotelSurroundingService.saveBatch(saveHotelSurroundings);
                            }

                            //更新进度
                            hotelImportTaskMapper.incrementProcessedCount(taskId, dataList.size());
                        }

                    })
                    .sheet()
                    .headRowNumber(1)
                    .doRead();

        } catch (Exception e) {
            hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            tempFile.delete();
        }

    }
}
