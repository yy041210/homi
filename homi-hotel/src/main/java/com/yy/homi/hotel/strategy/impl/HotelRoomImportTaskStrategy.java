package com.yy.homi.hotel.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.hotel.AlbumCategoryEnum;
import com.yy.homi.common.enums.hotel.AlbumSourceEnum;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import com.yy.homi.hotel.domain.entity.HotelRoom;
import com.yy.homi.hotel.feign.SysFileFeign;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.service.HotelAlbumService;
import com.yy.homi.hotel.service.HotelRoomService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class HotelRoomImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelAlbumService hotelAlbumService;
    @Autowired
    private HotelRoomService hotelRoomService;

    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;

    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;
    @Autowired
    private SysFileFeign sysFileFeign;

    @Override
    public String getTaskType() {
        return "HOTEL_ROOM";
    }

    @Async("homiExecutor")
    @Override
    public void execute(String taskId, String filePath, String userId) {
        log.info("开始执行酒店房型异步导入任务, TaskId: {}, File: {}", taskId, filePath);
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
        int BATCH_SIZE = 50;

        try {
            EasyExcel.read(filePath, new ReadListener<Map<Integer, String>>() {
                        @Override
                        public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
                            //读取每一行数据放入缓存
                            cachedDataList.add(data);
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
                            log.info("酒店母房型异步任务执行完成! taskId；{}", taskId);
                        }

                        @Override
                        public void onException(Exception exception, AnalysisContext context) throws Exception {
                            // 更新任务状态为失败
                            hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, "Excel读取异常! 异常信息："+ exception.getMessage());
                            throw exception;
                        }

                        private void processData(List<Map<Integer, String>> dataList) {
                            int currentBatchSize = dataList.size();
                            log.info("处理批次数据，当前批次大小：{}", currentBatchSize);
                            List<HotelRoom> hotelRooms = new ArrayList<>();  //需要保存的房型信息
                            List<HotelAlbum> hotelAlbums = new ArrayList<>();  //需要保存的酒店图集数据
                            List<HotelAlbum> updateHotelAlums = new ArrayList<>();  //需要修改的图集数据
                            Map<String, List<String>> imageUrlsMap = new HashMap<>(); //需要保存的房型关联的图片

                            //查询所有酒店id
                            Set<String> allHotelIds = hotelBaseMapper.selectList(null).stream().map(HotelBase::getId).collect(Collectors.toSet());
                            //查询所有酒店上传的图集
                            List<HotelAlbum> existHotelAlbums = hotelAlbumMapper
                                    .selectList(new LambdaQueryWrapper<HotelAlbum>()
                                            .eq(HotelAlbum::getSource, AlbumSourceEnum.HOTEL.getCode()) //酒店上传
                                    );
                            //查询所有房间
                            Set<String> allHotelIdRoomId = hotelRoomService.list().stream().map(hotelRoom -> hotelRoom.getHotelId() + "_" + hotelRoom.getId()).collect(Collectors.toSet());

                            for (Map<Integer, String> rowData : dataList) {
                                String hotelId = rowData.get(0);
                                String roomId = rowData.get(1);
                                String roomName = rowData.get(2);
                                Integer maxOccupancy = StrUtil.isEmpty(rowData.get(3)) ? null : Integer.parseInt(rowData.get(3));
                                String area = rowData.get(4);
                                String floor = rowData.get(5);
                                String bedType = rowData.get(6);
                                String window = rowData.get(7);
                                String wifi = rowData.get(8);
                                String smoke = rowData.get(9);
                                String heightLights = StrUtil.isEmpty(rowData.get(10)) ? null : StrUtil.join(",", rowData.get(10).trim().split("\\|"));

                                //检验必要字段
                                if (StrUtil.isEmpty(hotelId) || StrUtil.isEmpty(roomId) || StrUtil.isEmpty(roomName)) {
                                    log.warn("数据不完整，跳过：{}", rowData);
                                    continue;
                                }

                                if (!allHotelIds.contains(hotelId)) {
                                    log.warn("对应的酒店不存在，跳过：{}", rowData);
                                    continue;
                                }

                                List<String> imageUrls = StrUtil.isEmpty(rowData.get(11)) ? null : Arrays.asList(rowData.get(11).split(","));
                                if (CollectionUtil.isNotEmpty(imageUrls)) {
                                    imageUrlsMap.put(roomId, imageUrls);
                                }
                                //图集
                                int i = 0;
                                for (String imageUrl : imageUrls) {
                                    if (StrUtil.isEmpty(imageUrl)) {
                                        continue;
                                    }
                                    //hotelId + imageUrl + roomId + 酒店上传唯一
                                    List<HotelAlbum> existSameHotelAlbum = existHotelAlbums.stream()
                                            .filter(item -> item.getHotelId().equals(hotelId))
                                            .filter(item -> item.getImageUrl().equals(imageUrl))
                                            .filter(item -> item.getRoomId().equals(roomId))
                                            .collect(Collectors.toList());
                                    if (CollectionUtil.isNotEmpty(existSameHotelAlbum)) {
                                        continue;
                                    }


                                    List<HotelAlbum> hotelAlbumList = existHotelAlbums.stream()
                                            .filter(item -> item.getHotelId().equals(hotelId))
                                            .filter(item -> item.getImageUrl().equals(imageUrl))
                                            .filter(item -> item.getRoomId() == null)
                                            .collect(Collectors.toList());
                                    if (CollectionUtil.isNotEmpty(hotelAlbumList)) {
                                        //需要修改，添加上 roomId字段
                                        hotelAlbumList.stream().forEach(hotelAlbum -> {
                                            hotelAlbum.setRoomId(roomId);
                                            updateHotelAlums.add(hotelAlbum);
                                        });
                                    } else {
                                        HotelAlbum hotelAlbum = new HotelAlbum();
                                        hotelAlbum.setRoomId(roomId);
                                        hotelAlbum.setHotelId(hotelId);
                                        hotelAlbum.setSource(AlbumSourceEnum.HOTEL.getCode());
                                        hotelAlbum.setCategory(AlbumCategoryEnum.ROOM.getCode());  //房间分类
                                        hotelAlbum.setImageUrl(imageUrl);
                                        hotelAlbum.setCreateBy(userId);
                                        hotelAlbum.setCreateTime(new Date());
                                        hotelAlbum.setSeq(10 + i++);
                                        hotelAlbums.add(hotelAlbum);
                                    }
                                }

                                //基本房型数据
                                if (allHotelIdRoomId.contains(hotelId + "_" + roomId)) {
                                    continue;  //房型已经存在，跳过
                                }
                                HotelRoom hotelRoom = new HotelRoom();
                                hotelRoom.setId(roomId);
                                hotelRoom.setHotelId(hotelId);
                                hotelRoom.setName(roomName);
                                String areaNumStr = area.replaceAll("平方米", "");
                                hotelRoom.setAreaUnit("平方米");
                                if(areaNumStr.contains("–")){
                                    //10-20平方米
                                    int minArea = Integer.parseInt(areaNumStr.split("–")[0]);
                                    int maxArea = Integer.parseInt(areaNumStr.split("–")[1]);
                                    hotelRoom.setMinArea(minArea);
                                    hotelRoom.setMaxArea(maxArea);
                                    hotelRoom.setArea(minArea+"–"+maxArea+"平方米");
                                }else {
                                    //10平方米
                                    int areaNum = Integer.parseInt(areaNumStr);
                                    hotelRoom.setMinArea(areaNum);
                                    hotelRoom.setMinArea(areaNum);
                                    hotelRoom.setArea(areaNum+"平方米");
                                }

                                hotelRoom.setFloor(floor);
                                hotelRoom.setWifi(wifi);
                                hotelRoom.setBedType(bedType);
                                hotelRoom.setSmoke(smoke);
                                hotelRoom.setMaxOccupancy(maxOccupancy);
                                hotelRoom.setWindow(window);
                                hotelRoom.setHighlightFields(heightLights);
                                hotelRoom.setStatus(0);
                                hotelRoom.setCreateBy(userId);
                                Date now = new Date();
                                hotelRoom.setCreateTime(now);
                                hotelRoom.setUpdateBy(userId);
                                hotelRoom.setUpdateTime(now);

                                hotelRooms.add(hotelRoom);
                            }

                            //保存基本房型数据
                            hotelRoomService.saveBatch(hotelRooms);

                            //保存图片url
                            Set<String> saveImageUrls = imageUrlsMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());
                            R r = sysFileFeign.uploadBatchByUrls(new ArrayList<>(saveImageUrls));
                            if (r.getCode() != HttpStatus.OK.value()) {
                                hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, "酒店母房型异步任务执行过程中，从urls批量上传文件远程调用失败！");
                            }
                            Map<String, String> urlFileIdMap = (Map<String, String>) r.getData();  // k: url v: fileId

                            //酒店图集
                            hotelAlbums.stream()
                                    .filter(hotelAlbum -> urlFileIdMap.get(hotelAlbum.getImageUrl()) != null)
                                    .forEach(hotelAlbum -> hotelAlbum.setImageId(urlFileIdMap.get(hotelAlbum.getImageUrl())));

                            if(CollectionUtil.isNotEmpty(hotelAlbums)){
                                hotelAlbumService.saveBatch(hotelAlbums);
                            }

                            if(CollectionUtil.isNotEmpty(updateHotelAlums)){
                                hotelAlbumService.updateBatchById(updateHotelAlums);
                            }

                            hotelImportTaskMapper.incrementProcessedCount(taskId, dataList.size());
                        }
                    })
                    .sheet()
                    .headRowNumber(1)  //不要表头
                    .doRead();
        }catch (Exception e){
            hotelImportTaskMapper.updateToFailed(taskId,HotelImportTask.STATUS_FAILED,e.getMessage());
            throw new RuntimeException(e);
        }finally {
            //删除临时文件
            tempFile.delete();
        }

    }
}
