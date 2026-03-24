package com.yy.homi.hotel.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.hotel.AlbumSourceEnum;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.*;
import com.yy.homi.hotel.feign.SysFileFeign;
import com.yy.homi.hotel.mapper.*;
import com.yy.homi.hotel.service.HotelAlbumService;
import com.yy.homi.hotel.service.HotelCommentService;
import com.yy.homi.hotel.strategy.HotelImportTaskStrategy;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//酒店评论数据导入策略
@Slf4j
@Component
public class HotelCommentImportTaskStrategy implements HotelImportTaskStrategy {

    @Autowired
    private HotelCommentService hotelCommentService;

    @Autowired
    private HotelAlbumService hotelAlbumService;

    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelRoomMapper hotelRoomMapper;
    @Autowired
    private HotelCommentMapper hotelCommentMapper;
    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;
    @Autowired
    private SysFileFeign sysFileFeign;


    @Override
    public String getTaskType() {
        return "HOTEL_COMMENT";
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


            AtomicInteger seq = new AtomicInteger(0);
            // 使用 AtomicInteger 记录总处理数
            AtomicInteger totalProcessed = new AtomicInteger(0);
            // 使用 List 作为缓存
            List<Map<Integer, String>> cachedDataList = new ArrayList<>();
            // 批次大小
            int BATCH_SIZE = 1000;

            //查询所有酒店ids
            Set<String> allHotelIds = hotelBaseMapper.selectList(null).stream().map(HotelBase::getId).collect(Collectors.toSet());

            Map<String, String> allRoomHotelIdNameMap = hotelRoomMapper.selectList(null).stream().collect(Collectors.toMap(
                    item -> item.getHotelId() + item.getName(),
                    HotelRoom::getId
            ));

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

                            Set<String> allHotelCommentIds = hotelCommentMapper.selectList(null).stream().map(HotelComment::getId).collect(Collectors.toSet());

                            List<HotelComment> saveHotelComments = new ArrayList<>();  //需要保存的hotelComments
                            List<HotelAlbum> saveHotelAlbums = new ArrayList<>();        // 需要保存的hotelHotelAlbums
                            Set<String> saveFileUrls = new HashSet<>();                 //需要保存的文件uRL
                            for (Map<Integer, String> rowData : dataList) {
                                String id = rowData.get(0);
                                String hotelId = rowData.get(1);  //酒店id
                                //用户名id为空，爬取的数据没有该用户的id
                                String userName = rowData.get(2);  //用户名
                                String commentScore = rowData.get(3);   //评分
                                String commentContext = rowData.get(4); //评论内容
                                String checkInDateStr = rowData.get(5);  //入住时间
                                String publishDateStr = rowData.get(6);  //发布时间
                                String roomName = rowData.get(7);
                                String travelType = rowData.get(8);  //出游类型
                                String imageUrls = rowData.get(9); //图片集合
                                String videoUrls = rowData.get(10); //视频集合
                                String likeCountStr = rowData.get(11);  //点赞数

                                //检查字段
                                if (StrUtil.isEmpty(id) || StrUtil.isEmpty(hotelId) || StrUtil.isEmpty(userName) || StrUtil.isEmpty(commentContext)) {
                                    //数据不对，直接跳过
                                    continue;
                                }

                                //过滤酒店id不存在的数据或已经存在的评论
                                if (!allHotelIds.contains(hotelId) || allHotelCommentIds.contains(id)) {
                                    continue;
                                }
                                String roomId = allRoomHotelIdNameMap.get(hotelId + roomName);
                                if (StrUtil.isEmpty(roomId)) {
                                    log.warn("房间id为空！");
                                    continue;
                                }


                                //封装评论数据
                                HotelComment hotelComment = new HotelComment();
                                hotelComment.setId(id);
                                hotelComment.setHotelId(hotelId);
                                hotelComment.setUserId("");
                                hotelComment.setUserName(userName);
                                hotelComment.setCommentScore(Float.valueOf(commentScore));
                                hotelComment.setHygieneScore(Float.valueOf(commentScore));
                                hotelComment.setDeviceScore(Float.valueOf(commentScore));
                                hotelComment.setEnvironmentScore(Float.valueOf(commentScore));
                                hotelComment.setServiceScore(Float.valueOf(commentScore));
                                hotelComment.setCommentContext(commentContext);
                                hotelComment.setRoomId(roomId);
                                hotelComment.setRoomName(roomName);
                                hotelComment.setTravelType(travelType);
                                hotelComment.setLikeCount(Integer.parseInt(likeCountStr));

                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                //转换为 java.util.Date (如果实体类需要 Date 类型)
                                Date checkInTime = Date.from(LocalDateTime.parse(checkInDateStr, formatter).atZone(ZoneId.systemDefault()).toInstant());
                                Date publishTime = Date.from(LocalDateTime.parse(publishDateStr, formatter).atZone(ZoneId.systemDefault()).toInstant());
                                hotelComment.setCheckInTime(checkInTime);
                                hotelComment.setPublishTime(publishTime);

                                saveHotelComments.add(hotelComment);

                                //封装hotelAlbum数据
                                if (StrUtil.isNotEmpty(imageUrls)) {
                                    // 使用 Hutool 的 StrUtil.split 可以自动去空格和过滤空值
                                    List<String> list = StrUtil.split(imageUrls, '|', true, true);
                                    for (String imageUrl : list) {
                                        if (saveFileUrls.add(imageUrl)) {
                                            HotelAlbum hotelAlbum = new HotelAlbum();
                                            hotelAlbum.setHotelId(hotelId);
                                            hotelAlbum.setCommentId(id);
                                            hotelAlbum.setSeq(seq.incrementAndGet());
                                            hotelAlbum.setSource(AlbumSourceEnum.USER.getCode());
                                            Random random = new Random();
                                            hotelAlbum.setCategory(random.nextInt(9) + 1); // 生成 1-9 的随机整数 //随机分类
                                            hotelAlbum.setImageUrl(imageUrl);
                                            //imageId在后面添加
                                            saveHotelAlbums.add(hotelAlbum);
                                        }
                                    }
                                }
                                if (StrUtil.isNotEmpty(videoUrls)) {
                                    // 使用 Hutool 的 StrUtil.split 可以自动去空格和过滤空值
                                    List<String> list = StrUtil.split(videoUrls, '|', true, true);
                                    for (String videoUrl : list) {
                                        if (saveFileUrls.add(videoUrl)) {
                                            HotelAlbum hotelAlbum = new HotelAlbum();
                                            hotelAlbum.setHotelId(hotelId);
                                            hotelAlbum.setCommentId(id);
                                            hotelAlbum.setSeq(seq.incrementAndGet());
                                            hotelAlbum.setSource(AlbumSourceEnum.USER.getCode());
                                            Random random = new Random();
                                            hotelAlbum.setCategory(random.nextInt(9) + 1); // 生成 1-9 的随机整数 //随机分类
                                            hotelAlbum.setImageUrl(videoUrl);
                                            //imageId在后面添加
                                            saveHotelAlbums.add(hotelAlbum);
                                        }
                                    }
                                }


                            }

                            //通过urls保存文件
                            Map<String, String> result = new HashMap<>();
                            if (CollectionUtil.isNotEmpty(saveFileUrls)) {
                                R r = sysFileFeign.uploadBatchByUrls(new ArrayList<>(saveFileUrls));
                                if (r.getCode() != HttpStatus.OK.value()) {
                                    hotelImportTaskMapper.updateToFailed(taskId,HotelImportTask.STATUS_FAILED,"远程调用homi-file失败！错误信息："+r.getMsg());
                                    throw new ServiceException("远程调用文件服务，通过urls保存sysFile失败！");
                                }
                                result = (Map<String, String>) r.getData();
                            }


                            //保存hotelAlbum
                            Map<String, String> finalResult = result;
                            saveHotelAlbums = saveHotelAlbums.stream().filter(item -> {
                                String imageId = finalResult.get(item.getImageUrl());
                                if (StrUtil.isEmpty(imageId)) {
                                    return false;
                                }
                                item.setImageId(imageId);
                                return true;
                            }).collect(Collectors.toList());
                            hotelAlbumService.saveBatch(saveHotelAlbums);

                            //保存hotelComment
                            hotelCommentService.saveBatch(saveHotelComments);

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
