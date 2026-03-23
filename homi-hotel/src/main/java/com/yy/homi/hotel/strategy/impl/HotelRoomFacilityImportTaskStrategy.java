package com.yy.homi.hotel.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import com.yy.homi.hotel.domain.entity.HotelRoom;
import com.yy.homi.hotel.domain.entity.HotelRoomFacility;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.mapper.HotelRoomFacilityMapper;
import com.yy.homi.hotel.mapper.HotelRoomMapper;
import com.yy.homi.hotel.service.HotelRoomFacilityService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HotelRoomFacilityImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelRoomFacilityService hotelRoomFacilityService;
    @Autowired
    private HotelRoomMapper hotelRoomMapper;
    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;
    @Autowired
    private HotelRoomFacilityMapper hotelRoomFacilityMapper;

    @Override
    public String getTaskType() {
        return "HOTEL_ROOM_FACILITY";
    }

    @Override
    public void execute(String taskId, String filePath, String userId) {
        log.info("开始执行房型关联设备异步导入任务, TaskId: {}, File: {}", taskId, filePath);

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

        //查询所有roomId
        Set<String> allRoomIds = hotelRoomMapper.selectList(null).stream().map(HotelRoom::getId).collect(Collectors.toSet());

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
                            log.info("酒店房型关联设备导入 异步任务执行完成! taskId；{}", taskId);
                        }

                        @Override
                        public void onException(Exception exception, AnalysisContext context) throws Exception {
                            // 更新任务状态为失败
                            hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, "Excel读取异常! 异常信息：" + exception.getMessage());
                            throw exception;
                        }

                        void processData(List<Map<Integer, String>> dataList) {
                            if (CollectionUtil.isEmpty(dataList)) {
                                return;
                            }

                            List<HotelRoomFacility> saveHotelRoomFacilities = new ArrayList<>();

                            //这批数据的roomIds
                            Set<String> roomIds = dataList.stream().map(item -> item.get(1)).collect(Collectors.toSet());
                            Set<String> existRoomFacilityList = hotelRoomFacilityMapper
                                    .selectList(new LambdaQueryWrapper<HotelRoomFacility>()
                                            .in(HotelRoomFacility::getRoomId, roomIds)
                                    )
                                    .stream()
                                    .map(item -> item.getRoomId() + item.getFacilityType() + item.getName())
                                    .collect(Collectors.toSet());   //roomId + 洗漱用品 + 牙刷 集合，用于去重

                            for (Map<Integer, String> rowData : dataList) {
                                String roomId = rowData.get(1);
                                String facilityType = rowData.get(2);
                                String facilityName = rowData.get(3);
                                String statusStr = rowData.get(4);
                                Integer status = StrUtil.isEmpty(statusStr) ? null : Integer.parseInt(statusStr);
                                String tags = rowData.get(5);
                                String seqStr = rowData.get(6);
                                Integer seq = StrUtil.isEmpty(seqStr) ? null : Integer.parseInt(statusStr);

                                //参数校验
                                if (StrUtil.isBlank(roomId) || StrUtil.isBlank(facilityType) || StrUtil.isBlank(facilityName)) {
                                    continue;
                                }

                                //过滤不存在的房型
                                if(!allRoomIds.contains(roomId)){
                                    continue;
                                }

                                //roomId + 洗漱用品 + 牙刷 数据格式去重
                                if(existRoomFacilityList.contains(roomId+facilityType+facilityName)){
                                    continue;
                                }

                                HotelRoomFacility hotelRoomFacility = new HotelRoomFacility();
                                hotelRoomFacility.setRoomId(roomId);
                                hotelRoomFacility.setFacilityType(facilityType);
                                hotelRoomFacility.setName(facilityName);
                                hotelRoomFacility.setTags(tags);
                                hotelRoomFacility.setStatus(status);
                                hotelRoomFacility.setSeq(seq);
                                saveHotelRoomFacilities.add(hotelRoomFacility);
                            }

                            //保存酒店房型关联的设备信息
                            hotelRoomFacilityService.saveBatch(saveHotelRoomFacilities);

                            //修改任务进度
                            hotelImportTaskMapper.incrementProcessedCount(taskId, dataList.size());

                        }

                    })
                    .sheet()
                    .headRowNumber(1)  //不要表头
                    .doRead();
        } catch (Exception e) {
            hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, "EasyExcel执行异常，异常信息：" + e.getMessage());
        } finally {
            tempFile.delete(); // 删除临时文件
        }
    }
}
