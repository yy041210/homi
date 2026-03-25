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
import com.yy.homi.hotel.feign.SysFileFeign;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.service.HotelAlbumService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

//酒店图集异步导入任务
@Component
@Slf4j
public class HotelAlbumImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelAlbumService hotelAlbumService;

    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;

    @Autowired
    private SysFileFeign sysFileFeign;

    @Override
    public String getTaskType() {
        return "HOTEL_ALBUM";
    }

    @Override
    public void execute(String taskId, String filePath, String userId) {
        log.info("开始执行酒店图集异步导入任务, TaskId: {}, File: {}", taskId, filePath);
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

            //查询所有酒店id
            Set<String> allHotelIds = hotelBaseMapper.selectList(null).stream().map(HotelBase::getId).collect(Collectors.toSet());

            EasyExcel.read(tempFile, new ReadListener<Map<Integer, String>>() {

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

                @Override
                public void onException(Exception exception, AnalysisContext context) throws Exception {
                    // 更新任务状态为失败
                    hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, "Excel读取异常! 异常信息：" + exception.getMessage());
                    throw exception;
                }

                void processData(List<Map<Integer, String>> dataList) {

                    List<String> imageUrls = new ArrayList<>();  //需要保存的urls集合
                    List<HotelAlbum> saveHotelAlbums = new ArrayList<>();  //需要保存的hotelAlbums

                    for (Map<Integer, String> rowData : dataList) {
                        String seqStr = rowData.get(0);
                        String hotelId = rowData.get(1);
                        String sourceStr = rowData.get(2);
                        String categoryStr = rowData.get(3);
                        String imageId = rowData.get(4);
                        String imageUrl = rowData.get(5);
                        Integer seq = StrUtil.isEmpty(seqStr) ? 0 : Integer.parseInt(seqStr);

                        if (hotelId.isEmpty() || sourceStr.isEmpty() || categoryStr.isEmpty() || imageUrl.isEmpty()) {
                            continue;
                        }

                        Integer source = AlbumSourceEnum.fromDesc(sourceStr).getCode();
                        if (source == null) {
                            log.warn("未知的来源类型: " + sourceStr);
                            continue;
                        }

                        // 用户上传先不保存
                        if (source == AlbumSourceEnum.USER.getCode()) {
                            continue;
                        }

                        // 过滤不存在的酒店
                        if (!allHotelIds.contains(hotelId)) {
                            continue;
                        }

                        Integer category = AlbumCategoryEnum.fromDesc(categoryStr).getCode();
                        if (category == null) {
                            category = 9;
                            log.warn("imageId:{}: 未知的分类类型 '{}'，已默认设为'其他'", imageId, categoryStr);
                        }

                        if (StrUtil.isNotBlank(imageUrl)) {
                            imageUrls.add(imageUrl);
                        }

                        HotelAlbum album = new HotelAlbum();
                        album.setHotelId(hotelId);
                        album.setRoomId(null);
                        album.setCommentId(null);
                        album.setSeq(seq);
                        album.setSource(source);
                        album.setCategory(category);
                        album.setImageUrl(imageUrl);
                        album.setCreateBy(userId);
                        album.setCreateTime(new Date());
                        saveHotelAlbums.add(album);
                    }

                    // 处理图片URL
                    if (CollectionUtil.isNotEmpty(imageUrls)) {
                        R r = sysFileFeign.uploadBatchByUrls(new ArrayList<>(imageUrls));
                        if (r.getCode() != HttpStatus.OK.value()) {
                            hotelImportTaskMapper.updateToFailed(taskId,HotelImportTask.STATUS_FAILED,"根据urls集合存入数据库异常！");
                            throw new ServiceException("根据urls集合存入数据库异常！");
                        }
                        Map<String, String> urlIdMap = (Map<String, String>) r.getData();

                        // 设置图片id
                        saveHotelAlbums.stream()
                                .filter(album -> urlIdMap.containsKey(album.getImageUrl()))
                                .forEach(album -> album.setImageId(urlIdMap.get(album.getImageUrl())));
                    }

                    // 过滤已存在的数据
                    List<String> existHotelIds = saveHotelAlbums.stream()
                            .map(HotelAlbum::getHotelId).collect(Collectors.toList());

                    if (CollectionUtil.isNotEmpty(existHotelIds)) {
                        //酒店上传 + （hotelId + : + imageId）唯一去重
                        Set<String> existKeys = hotelAlbumMapper.selectList(
                                        new LambdaQueryWrapper<HotelAlbum>()
                                                .in(HotelAlbum::getHotelId, existHotelIds)
                                                .eq(HotelAlbum::getSource,AlbumSourceEnum.HOTEL.getCode()) //酒店上传

                                )
                                .stream()
                                .map(album -> album.getHotelId() + ":" + album.getImageId())
                                .collect(Collectors.toSet());

                        saveHotelAlbums = saveHotelAlbums.stream()
                                .filter(album -> {
                                    String key = album.getHotelId() + ":" + album.getImageId();
                                    if (existKeys.contains(key)) {
                                        return false;
                                    }
                                    return true;
                                })
                                .collect(Collectors.toList());
                    }

                    // 批量插入
                    if (!saveHotelAlbums.isEmpty()) {
                        hotelAlbumService.saveBatch(saveHotelAlbums);
                    }

                    hotelImportTaskMapper.incrementProcessedCount(taskId,dataList.size());

                }

            });

        } catch (Exception e) {
            hotelImportTaskMapper.updateToFailed(taskId, HotelImportTask.STATUS_FAILED, e.getMessage());
            throw e;
        } finally {
            tempFile.delete();
        }

    }
}
