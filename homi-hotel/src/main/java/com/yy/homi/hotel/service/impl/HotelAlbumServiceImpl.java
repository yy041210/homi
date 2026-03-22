package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.hotel.AlbumCategoryEnum;
import com.yy.homi.common.enums.hotel.AlbumSourceEnum;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.feign.SysFileFeign;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.service.HotelAlbumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HotelAlbumServiceImpl extends ServiceImpl<HotelAlbumMapper, HotelAlbum> implements HotelAlbumService {

    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;
    @Autowired
    private SysFileFeign sysFileFeign;

    @Override
    @Transactional
    public R importHotelAlbumFromCsv(MultipartFile file) {
        log.info("接收到CSV文件导入请求, 文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".csv") && !fileName.endsWith(".CSV"))) {
            return R.fail("请上传CSV格式文件");
        }

        // 统计变量
        List<HotelAlbum> successList = Collections.synchronizedList(new ArrayList<>());
        Set<String> imageUrls = new ConcurrentHashSet<>();
        List<Map<String, Object>> errorList = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger totalCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        // 查询所有酒店ids
        List<String> hotelIds = hotelBaseMapper.selectList(null).stream()
                .map(HotelBase::getId).collect(Collectors.toList());

        try {
            // 使用 Map<Integer, String> 监听器
            ReadListener<Map<Integer, String>> listener = new ReadListener<Map<Integer, String>>() {
                private static final int BATCH_COUNT = 100;
                private final List<String[]> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                private int currentRowNum = 1;

                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    currentRowNum++;

                    // 将 Map 转换为 String[]
                    String[] line = new String[data.size()];
                    for (int i = 0; i < data.size(); i++) {
                        line[i] = data.get(i);
                    }

                    cachedDataList.add(line);
                    totalCount.incrementAndGet();

                    if (cachedDataList.size() >= BATCH_COUNT) {
                        processBatch();
                        cachedDataList.clear();
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    if (!cachedDataList.isEmpty()) {
                        processBatch();
                        cachedDataList.clear();
                    }
                    log.info("所有数据解析完成，总计：{}条", totalCount.get());
                }

                private void processBatch() {
                    List<HotelAlbum> batchSuccessList = new ArrayList<>();

                    int startRowNum = currentRowNum - cachedDataList.size();

                    for (int i = 0; i < cachedDataList.size(); i++) {
                        String[] line = cachedDataList.get(i);
                        int lineNumber = startRowNum + i + 1;

                        try {
                            if (line.length < 6) {
                                throw new RuntimeException("CSV格式错误，需要6列数据，实际只有" + line.length + "列");
                            }

                            Integer seq = Integer.parseInt(line[0].trim());
                            String hotelId = line[1].trim();
                            String sourceStr = line[2].trim();
                            String categoryStr = line[3].trim();
                            String imageUrl = line[5].trim();

                            if (hotelId.isEmpty() || sourceStr.isEmpty() || categoryStr.isEmpty() || imageUrl.isEmpty()) {
                                throw new RuntimeException("必填字段不能为空");
                            }

                            Integer source = AlbumSourceEnum.fromDesc(sourceStr).getCode();
                            if (source == null) {
                                throw new RuntimeException("未知的来源类型: " + sourceStr);
                            }

                            // 用户上传先不保存
                            if (source == AlbumSourceEnum.USER.getCode()) {
                                continue;
                            }

                            // 过滤不存在的酒店
                            if (!hotelIds.contains(hotelId)) {
                                continue;
                            }

                            Integer category = AlbumCategoryEnum.fromDesc(categoryStr).getCode();
                            if (category == null) {
                                category = 9;
                                log.warn("第{}行: 未知的分类类型 '{}'，已默认设为'其他'", lineNumber, categoryStr);
                            }

                            if (StrUtil.isNotEmpty(imageUrl)) {
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
                            batchSuccessList.add(album);

                        } catch (Exception e) {
                            log.error("第{}行处理失败: {}", lineNumber, e.getMessage());
                            Map<String, Object> errorInfo = new HashMap<>();
                            errorInfo.put("line", lineNumber);
                            errorInfo.put("data", String.join(",", line));
                            errorInfo.put("reason", e.getMessage());
                            errorList.add(errorInfo);
                        }
                    }

                    // 处理图片URL
                    if (!imageUrls.isEmpty()) {
                        R r = sysFileFeign.uploadBatchByUrls(new ArrayList<>(imageUrls));
                        if (r.getCode() != HttpStatus.OK.value()) {
                            throw new ServiceException("根据urls集合存入数据库异常！");
                        }
                        Map<String, String> urlIdMap = (Map<String, String>) r.getData();

                        // 设置图片id
                        batchSuccessList.stream()
                                .filter(album -> urlIdMap.containsKey(album.getImageUrl()))
                                .forEach(album -> album.setImageId(urlIdMap.get(album.getImageUrl())));
                    }

                    // 过滤已存在的数据
                    List<String> existHotelIds = batchSuccessList.stream()
                            .map(HotelAlbum::getHotelId).collect(Collectors.toList());

                    if (!existHotelIds.isEmpty()) {
                        Set<String> existKeys = hotelAlbumMapper.selectList(
                                        new LambdaQueryWrapper<HotelAlbum>().in(HotelAlbum::getHotelId, existHotelIds))
                                .stream()
                                .map(album -> album.getHotelId() + ":" + album.getImageId())
                                .collect(Collectors.toSet());

                        batchSuccessList = batchSuccessList.stream()
                                .filter(album -> {
                                    String key = album.getHotelId() + ":" + album.getImageId();
                                    if (existKeys.contains(key)) {
                                        duplicateCount.incrementAndGet();
                                        return false;
                                    }
                                    return true;
                                })
                                .collect(Collectors.toList());
                    }

                    // 批量插入
                    if (!batchSuccessList.isEmpty()) {
                        saveBatch(batchSuccessList);
                        successList.addAll(batchSuccessList);
                    }
                }
            };

            // 使用Map监听器读取CSV文件
            EasyExcel.read(file.getInputStream(), listener)
                    .sheet()
                    .headRowNumber(1)
                    .doRead();

        } catch (IOException e) {
            log.error("读取CSV文件失败", e);
            return R.fail("读取CSV文件失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("导入失败", e);
            return R.fail("导入失败: " + e.getMessage());
        }

        String message = String.format("导入完成：总计%d条，成功%d条，重复%d条，失败%d条",
                totalCount.get(), successList.size(), duplicateCount.get(), errorList.size());

        log.info(message);

        Map<String, Object> result = new HashMap<>();
        result.put("total", totalCount.get());
        result.put("success", successList.size());
        result.put("duplicate", duplicateCount.get());
        result.put("failed", errorList.size());
        result.put("successList", successList);
        result.put("errorList", errorList);
        result.put("message", message);

        return R.ok(result);
    }
}