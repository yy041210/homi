package com.yy.homi.hotel.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson.JSON;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.AddressInfoTO;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import com.yy.homi.hotel.domain.entity.HotelStats;
import com.yy.homi.hotel.feign.AmapLocationFeign;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.service.HotelBaseService;
import com.yy.homi.hotel.service.HotelStatsService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HotelBaseImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelBaseService hotelBaseService;
    @Autowired
    private HotelStatsService hotelStatsService;

    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private AmapLocationFeign amapLocationFeign;

    @Override
    public String getTaskType() {
        return "HOTEL_BASE";
    }

    @Async(value = "homiExecutor")
    @Override
    public void execute(String taskId, String filePath, String userId) {
        log.info("开始执行酒店基本信息异步导入任务, TaskId: {}, File: {}", taskId, filePath);
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
                            log.info("酒店基本信息异步任务执行完成! taskId；{}", taskId);
                        }

                        void processData(List<Map<Integer, String>> dataList) {
                            //查询所有酒店id
                            Set<String> allHotelIds = hotelBaseMapper.selectList(null).stream().map(HotelBase::getId).collect(Collectors.toSet());

                            List<HotelBase> saveHotelBases = new ArrayList<>();
                            List<HotelStats> saveHotelStats = new ArrayList<>();
                            for (Map<Integer, String> rowData : dataList) {
                                //解析数据
                                String hotelId = rowData.get(0);
                                String name = rowData.get(1);
                                String nameEn = rowData.get(2);
                                String starStr = rowData.get(3);
                                Integer star = StrUtil.isEmpty(starStr) ? null : Integer.parseInt(starStr);
                                String latStr = rowData.get(4);  //纬度
                                Double lat = StrUtil.isEmpty(latStr) ? null : Double.valueOf(latStr);
                                String lngStr = rowData.get(5);   //经度
                                Double lng = StrUtil.isEmpty(lngStr) ? null : Double.valueOf(lngStr);
                                String address = rowData.get(6);
                                String hygieneScoreStr = rowData.get(7);
                                Float hygieneScore = StrUtil.isEmpty(hygieneScoreStr) ? null : Float.valueOf(hygieneScoreStr);
                                String deviceScoreStr = rowData.get(8);
                                Float deviceScore = StrUtil.isEmpty(deviceScoreStr) ? null : Float.valueOf(deviceScoreStr);
                                String environmentScoreStr = rowData.get(9);
                                Float environmentScore = StrUtil.isEmpty(environmentScoreStr) ? null : Float.valueOf(environmentScoreStr);
                                String serviceScoreStr = rowData.get(10);
                                Float serviceScore = StrUtil.isEmpty(serviceScoreStr) ? null : Float.valueOf(serviceScoreStr);
                                String commentScoreStr = rowData.get(11);
                                Float commentScore = StrUtil.isEmpty(commentScoreStr) ? null : Float.valueOf(commentScoreStr);
                                String commentDesc = rowData.get(12);
                                String commentCountStr = rowData.get(13);
                                Integer commentCount = StrUtil.isEmpty(commentCountStr) ? null : Integer.parseInt(commentCountStr);
                                String tagTitle = rowData.get(14);

                                //数据校验
                                if (StrUtil.isBlank(hotelId) || StrUtil.isBlank(name)) {
                                    continue;
                                }

                                //去重已经存在的酒店id
                                if (allHotelIds.contains(hotelId)) {
                                    continue;  //跳过
                                }

                                //酒店评分信息
                                HotelStats hotelStats = new HotelStats();
                                hotelStats.setHotelId(hotelId);
                                hotelStats.setHygieneScore(hygieneScore);
                                hotelStats.setDeviceScore(deviceScore);
                                hotelStats.setEnvironmentScore(environmentScore);
                                hotelStats.setServiceScore(serviceScore);
                                hotelStats.setCommentScore(commentScore);
                                hotelStats.setCommentCount(commentCount);
                                hotelStats.setCommentDescription(commentDesc);
                                hotelStats.setTagTitle(tagTitle);

                                saveHotelStats.add(hotelStats);

                                //酒店基本信息
                                HotelBase hotelBase = new HotelBase();
                                hotelBase.setId(hotelId);
                                hotelBase.setName(name);
                                hotelBase.setNameEn(nameEn);
                                hotelBase.setStar(star);
                                hotelBase.setAddress(address);
                                hotelBase.setLat(lat);
                                hotelBase.setLng(lng);
                                hotelBase.setStatus(CommonConstants.STATUS_ENABLED);
                                hotelBase.setCreateBy(userId);
                                hotelBase.setCreateTime(new Date());
                                hotelBase.setUpdateBy(userId);
                                hotelBase.setUpdateTime(new Date());

                                // 调用远程服务获取地址信息
                                if (lat != null && lng != null) {
                                    try {
                                        // 远程调用获取地址信息
                                        R r = amapLocationFeign.getAddressByLngLat(lng, lat);
                                        Thread.sleep(500);
                                        if (r != null && r.getCode() == 200 && r.getData() != null) {
                                            AddressInfoTO addressInfo = JSON.parseObject(JSON.toJSONString(r.getData()), AddressInfoTO.class);

                                            // 设置地区ID
                                            hotelBase.setProvinceId(addressInfo.getProvinceId());
                                            hotelBase.setCityId(addressInfo.getCityId());
                                            hotelBase.setDistrictId(addressInfo.getDistrictId());

                                            // 设置冗余字段（方便查询）
//                                    hotel.setProvinceName(addressInfo.getProvince());
//                                    hotel.setCityName(addressInfo.getCity());
                                            // hotel.setDistrictName(addressInfo.getDistrict());

                                            log.debug("酒店id:{} - 通过经纬度获取地区信息成功: 省={}, 市={}, 区={}", hotelId, addressInfo.getProvince(), addressInfo.getCity(), addressInfo.getDistrict());
                                        }
                                    } catch (Exception e) {
                                        log.error("酒店id：{} - 调用地区服务异常，HotelBase: {} - 异常信息：{}", hotelId, hotelBase, e.getMessage());
                                    }
                                }
                                saveHotelBases.add(hotelBase);

                            }

                            //插入酒店基本信息
                            hotelBaseService.saveBatch(saveHotelBases);

                            //插入酒店评分信息
                            hotelStatsService.saveBatch(saveHotelStats);

                            //修改任务进度
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
