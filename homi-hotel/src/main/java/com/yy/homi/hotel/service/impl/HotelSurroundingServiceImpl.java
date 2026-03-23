package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.hotel.SurroundingCategoryEnum;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelSurrounding;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelSurroundingMapper;
import com.yy.homi.hotel.service.HotelSurroundingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class HotelSurroundingServiceImpl extends ServiceImpl<HotelSurroundingMapper, HotelSurrounding> implements HotelSurroundingService {


    @Autowired
    private HotelSurroundingMapper hotelSurroundingMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;

    @Override
    public List<HotelSurrounding> findByHotelId(String hotelId) {
        return this.list(new LambdaQueryWrapper<HotelSurrounding>()
                .eq(HotelSurrounding::getHotelId, hotelId)
                .orderByAsc(HotelSurrounding::getSeq)
        );
    }

    @Override
    public List<HotelSurrounding> findByHotelIdAndCategory(String hotelId, Integer category) {
        return this.list(new LambdaQueryWrapper<HotelSurrounding>()
                .eq(HotelSurrounding::getHotelId, hotelId)
                .eq(HotelSurrounding::getCategory, category));
    }

    @Override
    public R importHotelSurroundingFromCsv(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            final List<HotelSurrounding> allEntities = new ArrayList<>();

            // 1. 读取 CSV 数据
            EasyExcel.read(is, new PageReadListener<Map<Integer, String>>(dataList -> {
                for (Map<Integer, String> line : dataList) {
                    if ("酒店ID".equals(line.get(0))) continue;

                    HotelSurrounding entity = new HotelSurrounding();
                    entity.setHotelId(line.get(0));
                    // 建议：加个判空保护
                    String categoryDesc = line.get(1);
                    entity.setCategory(SurroundingCategoryEnum.fromDesc(categoryDesc).getCode());
                    entity.setSurroundingName(line.get(2));
                    entity.setDistance(parseDistance(line.get(3)));
                    entity.setDistanceDesc(line.get(4));
                    entity.setArrivalType(line.get(5));
                    entity.setTagName(line.get(6));
                    parseLocation(line.get(7), entity);

                    allEntities.add(entity);
                }
            })).excelType(ExcelTypeEnum.CSV).sheet().doRead();

            if (CollectionUtil.isEmpty(allEntities)) return R.ok("文件为空");

            // 2. 内部去重 (去重 CSV 里的重复行)
            // Key: hotelId + name
            Map<String, HotelSurrounding> distinctMap = allEntities.stream()
                    .collect(Collectors.toMap(
                            item -> item.getHotelId() + ":" + item.getSurroundingName(),
                            item -> item,
                            (existing, replacement) -> existing // 遇到重复保留第一个
                    ));

            // 3. 准备过滤环境 (优化：只查当前涉及到的酒店ID)
            Set<String> batchHotelIds = distinctMap.values().stream()
                    .map(HotelSurrounding::getHotelId).collect(Collectors.toSet());

            // 校验：酒店必须存在
            Set<String> validHotelBaseIds = hotelBaseMapper.selectList(
                            new LambdaQueryWrapper<HotelBase>().in(HotelBase::getId, batchHotelIds))
                    .stream().map(HotelBase::getId).collect(Collectors.toSet());

            // 校验：数据库中是否已存在 (只查这些酒店的周边，防止内存溢出)
            Set<String> existingKeys = hotelSurroundingMapper.selectList(
                            new LambdaQueryWrapper<HotelSurrounding>().in(HotelSurrounding::getHotelId, batchHotelIds))
                    .stream().map(item -> item.getHotelId() + ":" + item.getSurroundingName())
                    .collect(Collectors.toSet());

            // 4. 执行最终过滤并设置 Seq
            AtomicInteger globalCount = new AtomicInteger(1);
            List<HotelSurrounding> finalSaveList = distinctMap.values().stream()
                    .filter(item -> validHotelBaseIds.contains(item.getHotelId())) // 注意：这里不要加 !
                    .filter(item -> !existingKeys.contains(item.getHotelId() + ":" + item.getSurroundingName()))
                    .peek(item -> item.setSeq(globalCount.getAndIncrement())) // 过滤后重新排序号
                    .collect(Collectors.toList());

            // 5. 批量保存
            if (CollectionUtil.isNotEmpty(finalSaveList)) {
                this.saveBatch(finalSaveList);
                return R.ok("导入完成，新增 " + finalSaveList.size() + " 条数据");
            }

            return R.ok("没有新数据需要导入");

        } catch (Exception e) {
            log.error("导入失败", e);
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    // 辅助方法：距离格式化 (2.2千米 -> 2200.0)
    private Double parseDistance(String raw) {
        if (raw == null || raw.isEmpty()) return 0.0;
        try {
            if (raw.contains("千米")) return Double.parseDouble(raw.replace("千米", "")) * 1000;
            if (raw.contains("米")) return Double.parseDouble(raw.replace("米", ""));
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // 辅助方法：解析经纬度 "115.21,36.08"
    private void parseLocation(String raw, HotelSurrounding entity) {
        if (raw != null && raw.contains(",")) {
            String[] split = raw.split(",");
            if (split.length == 2) {
                entity.setLon(Double.valueOf(split[0].trim()));
                entity.setLat(Double.valueOf(split[1].trim()));
            }
        }
    }
}