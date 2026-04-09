package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.hotel.SurroundingCategoryEnum;
import com.yy.homi.hotel.domain.entity.*;
import com.yy.homi.hotel.feign.SysCityFeign;
import com.yy.homi.hotel.mapper.*;
import com.yy.homi.hotel.service.AnalysisService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Autowired
    private HotelRoomMapper hotelRoomMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelStatsMapper hotelStatsMapper;
    @Autowired
    private HotelRoomFacilityMapper hotelRoomFacilityMapper;
    @Autowired
    private HotelFacilityMapper hotelFacilityMapper;
    @Autowired
    private HotelFacilityTypeMapper hotelFacilityTypeMapper;
    @Autowired
    private HotelSurroundingMapper hotelSurroundingMapper;
    @Autowired
    private UserActionLogMapper userActionLogMapper;

    @Autowired
    private SysCityFeign sysCityFeign;


    @Override
    public R getHotelStarDistribute() {
        // 1. 使用 MyBatis-Plus 进行分组统计
        QueryWrapper<HotelBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("star, count(*) as count")
                .groupBy("star");

        List<Map<String, Object>> maps = hotelBaseMapper.selectMaps(queryWrapper);

        // 2. 转换数据格式，适配前端饼状图（例如：{name: '5星', value: 10}）
        List<Map<String, Object>> result = maps.stream().map(map -> {
            Map<String, Object> item = new HashMap<>();
            Integer star = (Integer) map.get("star"); // 获取数据库中的星级数字

            String name;
            if (star == null) {
                name = "未评级";
            } else {
                // 使用 switch 语句进行中文映射
                switch (star) {
                    case 1:
                        name = "一星";
                        break;
                    case 2:
                        name = "二星";
                        break;
                    case 3:
                        name = "三星";
                        break;
                    case 4:
                        name = "四星";
                        break;
                    case 5:
                        name = "五星";
                        break;
                    default:
                        name = "未评级";
                        break;
                }
            }

            item.put("name", name);
            item.put("value", map.get("count"));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getCityHotelCount() {
        // 1. 分组统计各城市的酒店数量并按数量降序排列
        QueryWrapper<HotelBase> wrapper = new QueryWrapper<>();
        wrapper.select("city_id, count(*) as count")
                .groupBy("city_id")
                .orderByDesc("count")
                .last("limit 20"); // 限制数量，防止横向柱状图过长

        List<Map<String, Object>> maps = hotelBaseMapper.selectMaps(wrapper);
        if (CollectionUtils.isEmpty(maps)) {
            return R.ok(new ArrayList<>());
        }

        // 2. 提取所有非空的 city_id，并转为 List<Integer> 类型以匹配 Feign 接口
        List<Integer> cityIds = maps.stream()
                .map(m -> (Integer) m.get("city_id"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 3. 通过 Feign 远程获取城市 ID 与名称的映射
        // 注意：假设返回的 R 对象中 data 结构为 Map<Integer, String> 或 Map<String, String>
        R r = sysCityFeign.getNamesByIds(cityIds);
        Map<Object, String> cityIdNameMap = (Map<Object, String>) r.getData();

        // 4. 封装最终的横向柱状图数据结构
        List<Map<String, Object>> result = maps.stream().map(map -> {
            Map<String, Object> item = new HashMap<>();
            Integer cityId = (Integer) map.get("city_id");

            // 从映射中获取名称，需注意 Map 的 Key 类型（JSON 序列化后 Key 可能会变成 String）
            String cityName = "未知城市";
            if (cityIdNameMap != null) {
                // 兼容处理：尝试用 Integer 找，找不到再尝试用 String 找
                cityName = cityIdNameMap.getOrDefault(cityId,
                        cityIdNameMap.getOrDefault(String.valueOf(cityId), "未知城市"));
            }

            item.put("name", cityName);  // 对应类目轴（yAxis）
            item.put("value", map.get("count")); // 对应数值轴（xAxis）
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getOpenYearDistribute() {
        // 1. 分组统计开业年份，并按年份升序排列
        QueryWrapper<HotelBase> wrapper = new QueryWrapper<>();
        wrapper.select("open_year as openYear, count(*) as count")
                .isNotNull("open_year") // 过滤掉年份为空的数据
                .groupBy("open_year")
                .orderByAsc("open_year"); // 年份从小到大排列

        List<Map<String, Object>> maps = hotelBaseMapper.selectMaps(wrapper);

        // 2. 转换数据格式，适配前端图表
        // 结构：[{name: "2010", value: 5}, {name: "2011", value: 8}]
        List<Map<String, Object>> result = maps.stream().map(map -> {
            Map<String, Object> item = new HashMap<>();
            // 注意：数据库字段名可能因为别名变成小写或驼峰，需对应
            Object yearObj = map.get("openYear");

            item.put("name", yearObj != null ? yearObj.toString() : "未知");
            item.put("value", map.get("count"));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getCommentScoreDistribute() {
        // 1. 定义评分区间 SQL 逻辑
        String sql = "CASE " +
                "WHEN s.comment_score >= 1.0 AND s.comment_score < 2.0 THEN '1.0-2.0' " +
                "WHEN s.comment_score >= 2.0 AND s.comment_score < 3.0 THEN '2.0-3.0' " +
                "WHEN s.comment_score >= 3.0 AND s.comment_score < 3.5 THEN '3.0-3.5' " +
                "WHEN s.comment_score >= 3.5 AND s.comment_score < 4.0 THEN '3.5-4.0' " +
                "WHEN s.comment_score >= 4.0 AND s.comment_score < 4.5 THEN '4.0-4.5' " +
                "WHEN s.comment_score >= 4.5 AND s.comment_score <= 5.0 THEN '4.5-5.0' " +
                "ELSE 'OTHER' END";

        QueryWrapper<HotelBase> wrapper = new QueryWrapper<>();
        // 2. 关联 hotel_stats 表查询
        wrapper.select(sql + " as scoreRange, count(h.id) as count")
                .last("as h LEFT JOIN hotel_stats s ON h.id = s.hotel_id " +
                        "GROUP BY scoreRange");

        List<Map<String, Object>> maps = hotelBaseMapper.selectMaps(wrapper);

        // 3. 固定区间顺序
        List<String> targetOrder = Arrays.asList(
                "1.0-2.0", "2.0-3.0", "3.0-3.5", "3.5-4.0", "4.0-4.5", "4.5-5.0"
        );

        // 4. 过滤掉“其他”并按顺序封装
        List<Map<String, Object>> result = targetOrder.stream().map(range -> {
            Map<String, Object> item = new HashMap<>();
            // 从查询结果中匹配对应的区间数量
            Long count = maps.stream()
                    .filter(m -> range.equals(m.get("scoreRange")))
                    .map(m -> (Long) m.get("count"))
                    .findFirst()
                    .orElse(0L); // 如果该区间没数据，补 0

            item.put("name", range);
            item.put("value", count);
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getHotelRadarStats() {
        // 1. 编写聚合查询 SQL，计算各维度平均分
        // 星级(star)来自 hotel_base，其余评分来自 hotel_stats
        QueryWrapper<HotelBase> wrapper = new QueryWrapper<>();
        wrapper.select(
                "AVG(s.hygiene_score) as hygiene",
                "AVG(s.device_score) as device",
                "AVG(s.environment_score) as environment",
                "AVG(s.service_score) as service",
                "AVG(h.star) as star"
        ).last("as h LEFT JOIN hotel_stats s ON h.id = s.hotel_id");

        Map<String, Object> map = hotelBaseMapper.selectMaps(wrapper).get(0);

        // 2. 构造雷达图所需的数据格式
        // 维度顺序：卫生，设施，环境，服务，星级
        List<Double> data = new ArrayList<>();
        data.add(formatAverage(map.get("hygiene")));
        data.add(formatAverage(map.get("device")));
        data.add(formatAverage(map.get("environment")));
        data.add(formatAverage(map.get("service")));
        data.add(formatAverage(map.get("star")));

        // 构造返回给前端的对象
        Map<String, Object> result = new HashMap<>();
        result.put("values", data);
        result.put("indicators", Arrays.asList("卫生", "设施", "环境", "服务", "星级"));

        return R.ok(result);
    }

    @Override
    public R getCommentCountDistribute() {
        // 1. 定义评论数区间逻辑
        String sql = "CASE " +
                "WHEN comment_count = 0 THEN '0' " +
                "WHEN comment_count > 0 AND comment_count <= 50 THEN '1-50' " +
                "WHEN comment_count > 50 AND comment_count <= 100 THEN '51-100' " +
                "WHEN comment_count > 100 AND comment_count <= 500 THEN '101-500' " +
                "WHEN comment_count > 500 AND comment_count <= 1000 THEN '501-1000' " +
                "WHEN comment_count > 1000 THEN '1000+' " +
                "ELSE 'OTHER' END";

        QueryWrapper<HotelStats> wrapper = new QueryWrapper<>();
        wrapper.select(sql + " as countRange, count(*) as num")
                .groupBy("countRange");

        List<Map<String, Object>> maps = hotelStatsMapper.selectMaps(wrapper);

        // 2. 规定显示顺序
        List<String> sortOrder = Arrays.asList("0", "1-50", "51-100", "101-500", "501-1000", "1000+");

        // 3. 转换并排序数据
        List<Map<String, Object>> result = sortOrder.stream().map(range -> {
            Map<String, Object> item = new HashMap<>();
            Long count = maps.stream()
                    .filter(m -> range.equals(m.get("countRange")))
                    .map(m -> (Long) m.get("num"))
                    .findFirst()
                    .orElse(0L);

            item.put("name", range);
            item.put("value", count);
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }


    @Override
    public R getRoomPriceDistribute() {
        // 1. 定义价格区间划分 SQL 逻辑
        String sql = "CASE " +
                "WHEN price >= 0 AND price < 150 THEN '0-150' " +
                "WHEN price >= 150 AND price < 300 THEN '150-300' " +
                "WHEN price >= 300 AND price < 500 THEN '300-500' " +
                "WHEN price >= 500 AND price < 1000 THEN '500-1000' " +
                "WHEN price >= 1000 THEN '1000+' " +
                "ELSE '未知' END";

        QueryWrapper<HotelRoom> wrapper = new QueryWrapper<>();
        wrapper.select(sql + " as priceRange, count(*) as count")
                .groupBy("priceRange");

        // hotelRoomMapper 需提前注入
        List<Map<String, Object>> maps = hotelRoomMapper.selectMaps(wrapper);

        // 2. 规定显示顺序
        List<String> sortOrder = Arrays.asList("0-150", "150-300", "300-500", "500-1000", "1000+");

        // 3. 转换并排序数据，处理空区间
        List<Map<String, Object>> result = sortOrder.stream().map(range -> {
            Map<String, Object> item = new HashMap<>();
            Long count = maps.stream()
                    .filter(m -> range.equals(m.get("priceRange")))
                    .map(m -> (Long) m.get("count"))
                    .findFirst()
                    .orElse(0L); // 某区间无房型时补0

            item.put("name", range + "元");
            item.put("value", count);
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getBedTypeDistribute() {
        // 1. 定义模糊匹配分类逻辑
        String sql = "CASE " +
                // 1. 多床/家庭房 (优先级最高，防止被拆分为单床)
                "WHEN bed_type LIKE '%三床%' OR bed_type LIKE '%3张%' OR bed_type LIKE '%家庭%' THEN '多床房/家庭房' " +
                // 2. 双床房
                "WHEN bed_type LIKE '%双床%' OR bed_type LIKE '%2张%' THEN '双床房' " +
                // 3. 特大床 (通常指2.0米及以上)
                "WHEN bed_type LIKE '%特大%' OR bed_type LIKE '%2.0米%' OR bed_type LIKE '%2米%' THEN '特大床房' " +
                // 4. 榻榻米 (独立分类或归入大床)
                "WHEN bed_type LIKE '%榻榻米%' THEN '榻榻米' " +
                // 5. 标准大床 (包含1.5米、1.8米等)
                "WHEN bed_type LIKE '%大床%' OR bed_type LIKE '%1张%' OR bed_type LIKE '%单床%' THEN '大床房' " +
                // 6. 套房
                "WHEN bed_type LIKE '%套房%' THEN '套房' " +
                "ELSE '其他床型' END";

        QueryWrapper<HotelRoom> wrapper = new QueryWrapper<>();
        wrapper.select(sql + " as bedCategory, count(*) as count")
                .groupBy("bedCategory");

        List<Map<String, Object>> maps = hotelRoomMapper.selectMaps(wrapper);

        // 2. 转换数据格式适配饼状图
        List<Map<String, Object>> result = maps.stream().map(map -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", map.get("bedCategory"));
            item.put("value", map.get("count"));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getRoomAreaDistribute() {
        // 1. 定义面积区间划分 SQL 逻辑
        String avgAreaSql = "((min_area + max_area) / 2.0)";

        String sql = "CASE " +
                "WHEN " + avgAreaSql + " < 15 THEN '15㎡以下' " +
                "WHEN " + avgAreaSql + " >= 15 AND " + avgAreaSql + " < 30 THEN '15-30㎡' " +
                "WHEN " + avgAreaSql + " >= 30 AND " + avgAreaSql + " < 50 THEN '30-50㎡' " +
                "WHEN " + avgAreaSql + " >= 50 AND " + avgAreaSql + " < 80 THEN '50-80㎡' " +
                "WHEN " + avgAreaSql + " >= 80 THEN '80㎡以上' " +
                "ELSE '未知' END";

        QueryWrapper<HotelRoom> wrapper = new QueryWrapper<>();
        wrapper.select(sql + " as areaRange, count(*) as count")
                .isNotNull("min_area")
                .groupBy("areaRange");

        List<Map<String, Object>> maps = hotelRoomMapper.selectMaps(wrapper);

        // 2. 规定显示顺序
        List<String> sortOrder = Arrays.asList("15㎡以下", "15-30㎡", "30-50㎡", "50-80㎡", "80㎡以上");

        // 3. 转换并排序数据
        List<Map<String, Object>> result = sortOrder.stream().map(range -> {
            Map<String, Object> item = new HashMap<>();
            Long count = maps.stream()
                    .filter(m -> range.equals(m.get("areaRange")))
                    .map(m -> (Long) m.get("count"))
                    .findFirst()
                    .orElse(0L);

            item.put("name", range);
            item.put("value", count);
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getRoomBubbleStats() {
        // 1. 查询房型数据，过滤掉面积或价格为空的异常数据
        QueryWrapper<HotelRoom> wrapper = new QueryWrapper<>();
        wrapper.select("id", "name", "min_area", "max_area", "price", "max_occupancy")
                .isNotNull("min_area")
                .isNotNull("price")
                .last("limit 500"); // 限制 500 条，保证前端渲染性能

        List<HotelRoom> list = hotelRoomMapper.selectList(wrapper);

        // 2. 转换为气泡图坐标数组 [x, y, size, name]
        List<Object[]> result = list.stream().map(room -> {
            // 计算平均面积作为 X 轴
            double avgArea = (room.getMinArea() + room.getMaxArea()) / 2.0;
            // 价格作为 Y 轴
            double price = room.getPrice();
            // 最大入住人数决定气泡大小
            int size = room.getMaxOccupancy() != null ? room.getMaxOccupancy() : 2;
            // 房型名称用于 Tooltip 显示
            String name = room.getName();

            return new Object[]{avgArea, price, size, name};
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getRoomFacilityWordCloud() {
        // 1. 统计设施名称出现的频率
        QueryWrapper<HotelRoomFacility> wrapper = new QueryWrapper<>();
        wrapper.select("name, count(*) as count")
                .eq("status", 0) // 只统计启用的设施
                .groupBy("name")
                .orderByDesc("count")
                .last("limit 100"); // 词云图通常展示前 100 个关键词

        List<Map<String, Object>> maps = hotelRoomFacilityMapper.selectMaps(wrapper);

        // 2. 转换为前端词云图所需的格式
        List<Map<String, Object>> result = maps.stream().map(map -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", map.get("name"));
            item.put("value", map.get("count"));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getFacilityTypeDistribute() {
        // 1. 统计各设施类型的数量
        QueryWrapper<HotelFacility> wrapper = new QueryWrapper<>();
        wrapper.select("hotel_facility_type_id, count(*) as count")
                .eq("status", 0) // 只统计启用的设施
                .groupBy("hotel_facility_type_id");

        List<Map<String, Object>> maps = hotelFacilityMapper.selectMaps(wrapper);
        if (CollectionUtils.isEmpty(maps)) {
            return R.ok(new ArrayList<>());
        }

        // 2. 提取所有类型 ID 并批量获取名称
        List<String> typeIds = maps.stream()
                .map(m -> (String) m.get("hotel_facility_type_id"))
                .collect(Collectors.toList());

        // 假设 facilityTypeService 提供了按 ID 集合查询的方法
        List<HotelFacilityType> types = hotelFacilityTypeMapper.selectBatchIds(typeIds);
        Map<String, String> typeNameMap = types.stream()
                .collect(Collectors.toMap(HotelFacilityType::getId, HotelFacilityType::getName));

        // 3. 封装为玫瑰图数据格式
        List<Map<String, Object>> result = maps.stream().map(map -> {
            Map<String, Object> item = new HashMap<>();
            String typeId = (String) map.get("hotel_facility_type_id");
            String typeName = typeNameMap.getOrDefault(typeId, "未知类型");

            item.put("name", typeName);
            item.put("value", map.get("count"));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getHotelFacilityWordCloud() {
        // 1. 统计设施名称出现的频次
        QueryWrapper<HotelFacility> wrapper = new QueryWrapper<>();
        wrapper.select("name, count(*) as value")
                .eq("status", 0) // 仅统计启用的设施
                .groupBy("name")
                .orderByDesc("value")
                .last("limit 80"); // 词云图建议展示前80个高频词

        List<Map<String, Object>> maps = hotelFacilityMapper.selectMaps(wrapper);

        // 2. 转换数据格式
        List<Map<String, Object>> result = maps.stream().map(map -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", map.get("name"));
            item.put("value", map.get("value"));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getSurroundingDistanceDistribute() {
        // 1. 定义距离区间划分 SQL 逻辑
        String distanceSql = "CASE " +
                "WHEN distance < 500 THEN '500m内' " +
                "WHEN distance >= 500 AND distance < 1000 THEN '500m-1km' " +
                "WHEN distance >= 1000 AND distance < 2000 THEN '1km-2km' " +
                "WHEN distance >= 2000 AND distance < 5000 THEN '2km-5km' " +
                "WHEN distance >= 5000 THEN '5km以上' " +
                "ELSE '未知' END";

        // 2. 聚合统计：按类型和距离区间分组
        QueryWrapper<HotelSurrounding> wrapper = new QueryWrapper<>();
        wrapper.select("category, " + distanceSql + " as distRange, count(*) as num")
                .groupBy("category", "distRange");

        List<Map<String, Object>> maps = hotelSurroundingMapper.selectMaps(wrapper);

        // 3. 准备前端所需的数据结构
        List<String> xLabels = Arrays.asList("500m内", "500m-1km", "1km-2km", "2km-5km", "5km以上");

        // 按类别组织数据
        Map<String, List<Long>> seriesData = new HashMap<>();

        // 初始化各个分类的数组
        for (SurroundingCategoryEnum cat : SurroundingCategoryEnum.values()) {
            if (cat == SurroundingCategoryEnum.OTHER) continue; // 可选排除其他
            seriesData.put(cat.getDesc(), new ArrayList<>(Collections.nCopies(xLabels.size(), 0L)));
        }

        // 4. 填充数据
        for (Map<String, Object> map : maps) {
            Integer categoryCode = Integer.parseInt(map.get("category").toString());
            String catDesc = SurroundingCategoryEnum.getDescByCode(categoryCode);
            String distRange = (String) map.get("distRange");
            Long count = (Long) map.get("num");

            if (catDesc != null && seriesData.containsKey(catDesc) && xLabels.contains(distRange)) {
                int index = xLabels.indexOf(distRange);
                seriesData.get(catDesc).set(index, count);
            }
        }

        // 5. 组装返回对象
        Map<String, Object> result = new HashMap<>();
        result.put("xLabels", xLabels);
        result.put("series", seriesData);

        return R.ok(result);
    }

    @Override
    public R getStarSurroundingAvg() {
        // 1. 定义 SQL 逻辑
        String selectSql = "h.star, " +
                "COUNT(CASE WHEN s.category = 3 THEN 1 END) * 1.0 / COUNT(DISTINCT h.id) as foodAvg, " +
                "COUNT(CASE WHEN s.category = 1 THEN 1 END) * 1.0 / COUNT(DISTINCT h.id) as trafficAvg, " +
                "COUNT(CASE WHEN s.category = 4 THEN 1 END) * 1.0 / COUNT(DISTINCT h.id) as shopAvg, " +
                "COUNT(CASE WHEN s.category = 2 THEN 1 END) * 1.0 / COUNT(DISTINCT h.id) as scenicAvg";

        QueryWrapper<HotelBase> wrapper = new QueryWrapper<>();
        // 只查询我们定义的统计字段，防止 MyBatis-Plus 自动添加不带前缀的 id 字段
        wrapper.select(selectSql);
        wrapper.last("as h LEFT JOIN hotel_surrounding s ON h.id = s.hotel_id COLLATE utf8mb4_general_ci " +
                "WHERE h.star BETWEEN 1 AND 5 " +
                "GROUP BY h.star " +
                "ORDER BY h.star ASC");

        List<Map<String, Object>> maps = hotelBaseMapper.selectMaps(wrapper);

        // 2. 初始化结果容器：确保每个分类都有 5 个位置（对应 1-5 星）
        // 使用 LinkedHashMap 保证 JSON 键值对顺序（餐饮->交通->购物->景点）
        Map<String, List<Double>> result = new LinkedHashMap<>();
        String[] categoryLabels = {"餐饮", "交通", "购物", "景点"};

        for (String label : categoryLabels) {
            List<Double> dataList = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                dataList.add(0.0); // 默认填充 0.0
            }
            result.put(label, dataList);
        }

        // 3. 将数据库查询到的实际数据填充到 List 的对应索引位置
        for (Map<String, Object> map : maps) {
            // 获取当前行代表的星级（1-5）
            Object starObj = map.get("star");
            if (starObj != null) {
                int star = Integer.parseInt(starObj.toString());
                // 索引 = 星级 - 1 (例如 3星 对应 List 索引 2)
                int index = star - 1;

                if (index >= 0 && index < 5) {
                    result.get("餐饮").set(index, formatAverage(map.get("foodAvg")));
                    result.get("交通").set(index, formatAverage(map.get("trafficAvg")));
                    result.get("购物").set(index, formatAverage(map.get("shopAvg")));
                    result.get("景点").set(index, formatAverage(map.get("scenicAvg")));
                }
            }
        }

        return R.ok(result);
    }

    @Override
    public R getUserActiveTimeDistribution() {
        // 1. 构建 SQL：按小时分组统计 24 小时内各行为数量
        // 使用 CASE WHEN 结合实体类中定义的常量进行统计
        String selectSql = "HOUR(create_time) as hour, " +
                "COUNT(CASE WHEN action_type = 'VIEW_DETAIL' THEN 1 END) as viewCount, " +
                "COUNT(CASE WHEN action_type = 'SEARCH' THEN 1 END) as searchCount, " +
                "COUNT(CASE WHEN action_type = 'FAVORITE' THEN 1 END) as favoriteCount, " +
                "COUNT(CASE WHEN action_type = 'CLICK_TRIP' THEN 1 END) as bookingCount";

        QueryWrapper<UserActionLog> wrapper = new QueryWrapper<>();
        wrapper.select(selectSql)
                .groupBy("hour")
                .orderByAsc("hour");

        List<Map<String, Object>> maps = userActionLogMapper.selectMaps(wrapper);

        // 2. 初始化结果容器：LinkedHashMap 保证顺序，List 预填 24 个 0
        Map<String, List<Long>> result = new LinkedHashMap<>();
        String[] labels = {"浏览", "搜索", "收藏", "预定"};
        for (String label : labels) {
            // 初始化 24 小时数据为 0
            List<Long> hoursData = new ArrayList<>(Collections.nCopies(24, 0L));
            result.put(label, hoursData);
        }

        // 3. 填充查询到的真实数据
        for (Map<String, Object> map : maps) {
            if (map.get("hour") != null) {
                int hour = Integer.parseInt(map.get("hour").toString());

                // 将数据库统计值填入 List 对应索引 (0-23)
                if (hour >= 0 && hour < 24) {
                    result.get("浏览").set(hour, getLongValue(map.get("viewCount")));
                    result.get("搜索").set(hour, getLongValue(map.get("searchCount")));
                    result.get("收藏").set(hour, getLongValue(map.get("favoriteCount")));
                    result.get("预定").set(hour, getLongValue(map.get("bookingCount")));
                }
            }
        }

        return R.ok(result);
    }

    @Override
    public R getUserStarPreference() {
        // 1. 构建 SQL：只统计行为类型为 'VIEW_DETAIL' 的记录
        // 按照 star 分组并计算每组的数量
        QueryWrapper<UserActionLog> wrapper = new QueryWrapper<>();
        wrapper.select("star", "count(*) as count")
                .eq("action_type", UserActionLog.VIEW_ACTION) // 过滤浏览行为
                .isNotNull("star")
                .groupBy("star")
                .orderByAsc("star");

        List<Map<String, Object>> maps = userActionLogMapper.selectMaps(wrapper);

        // 2. 数据格式化：确保 1-5 星都有数据，没有则补 0
        // 定义一个有序 Map 存储结果，Key 为星级名称，Value 为点击次数
        Map<String, Long> result = new LinkedHashMap<>();
        String[] starLabels = {"一星", "二星", "三星", "四星", "五星"};
        for (String label : starLabels) {
            result.put(label, 0L);
        }

        // 3. 将查询结果填充到 Map 中
        for (Map<String, Object> map : maps) {
            int star = Integer.parseInt(map.get("star").toString());
            long count = ((Number) map.get("count")).longValue();

            // star 1-5 对应 starLabels 的索引 0-4
            if (star >= 1 && star <= 5) {
                result.put(starLabels[star - 1], count);
            }
        }

        return R.ok(result);
    }

    @Override
    public R getTopBrowsedCities() {
        // 1. 从日志表统计浏览量 Top 10 的 city_id
        QueryWrapper<UserActionLog> wrapper = new QueryWrapper<>();
        wrapper.select("city_id", "count(*) as viewCount")
                .eq("action_type", UserActionLog.VIEW_ACTION)
                .isNotNull("city_id")
                .groupBy("city_id")
                .orderByDesc("viewCount")
                .last("LIMIT 10");

        List<Map<String, Object>> maps = userActionLogMapper.selectMaps(wrapper);
        if (maps == null || maps.isEmpty()) {
            return R.ok(new HashMap<>());
        }

        // 2. 提取 cityIds 并通过 Feign 获取城市名称映射
        List<Integer> cityIds = maps.stream()
                .map(m -> Integer.parseInt(m.get("city_id").toString()))
                .collect(Collectors.toList());

        // 调用远程 Feign 接口
        R r = sysCityFeign.getNamesByIds(cityIds);
        if(r.getCode()!= HttpStatus.OK.value()){
            return R.fail("远程调用rbac失败！");
        }
        Map<String, String> cityIdMap = (Map<String, String>) r.getData();

        // 3. 组装最终返回给前端的数据 (cityNames 数组 + counts 数组)
        List<String> cityNames = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        for (Map<String, Object> map : maps) {
            String cityId = map.get("city_id").toString();
            long count = ((Number) map.get("viewCount")).longValue();

            // 从 Feign 返回的 Map 中获取名称，取不到则用日志里的冗余名或占位符
            String cityName = cityIdMap != null ? cityIdMap.get(cityId) : "未知城市";

            cityNames.add(cityName);
            counts.add(count);
        }

        // 4. 反转数组，适配 Echarts 横向柱状图（让第一名显示在最上方）
        Collections.reverse(cityNames);
        Collections.reverse(counts);

        Map<String, Object> result = new HashMap<>();
        result.put("cityNames", cityNames);
        result.put("counts", counts);

        return R.ok(result);
    }

    @Override
    public R getUserPricePreference() {
        // 1. 构建 SQL：使用 CASE WHEN 根据你指定的区间进行切分
        // 这里的 show_price 对应 UserActionLog 实体类中的价格字段
        String selectSql = "CASE " +
                "WHEN show_price < 100 THEN '100元以下' " +
                "WHEN show_price >= 100 AND show_price < 200 THEN '100-200元' " +
                "WHEN show_price >= 200 AND show_price < 300 THEN '200-300元' " +
                "WHEN show_price >= 300 AND show_price < 400 THEN '300-400元' " +
                "WHEN show_price >= 400 AND show_price < 600 THEN '400-600元' " +
                "WHEN show_price >= 600 AND show_price < 1000 THEN '600-1000元' " +
                "WHEN show_price >= 1000 THEN '1000元以上' " +
                "ELSE '其他' END as priceRange, " +
                "count(*) as count";

        QueryWrapper<UserActionLog> wrapper = new QueryWrapper<>();
        wrapper.select(selectSql)
                .eq("action_type", UserActionLog.VIEW_ACTION) // 仅统计浏览行为
                .isNotNull("show_price")
                .groupBy("priceRange");

        List<Map<String, Object>> maps = userActionLogMapper.selectMaps(wrapper);

        // 2. 初始化结果容器：使用 LinkedHashMap 严格固定前端显示的区间顺序
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("100元以下", 0L);
        result.put("100-200元", 0L);
        result.put("200-300元", 0L);
        result.put("300-400元", 0L);
        result.put("400-600元", 0L);
        result.put("600-1000元", 0L);
        result.put("1000元以上", 0L);

        // 3. 将数据库查询结果填充到对应的区间中
        if (maps != null) {
            for (Map<String, Object> map : maps) {
                Object rangeKey = map.get("priceRange");
                Object countVal = map.get("count");

                if (rangeKey != null && result.containsKey(rangeKey.toString())) {
                    result.put(rangeKey.toString(), ((Number) countVal).longValue());
                }
            }
        }

        return R.ok(result);
    }

    @Override
    public R getUserActionFunnel() {
        // 1. 定义行为链路及其对应的常量
        // 链路顺序：搜索 -> 浏览 -> 收藏 -> 预定(跳转)
        Map<String, String> actionSteps = new LinkedHashMap<>();
        actionSteps.put("搜索", UserActionLog.SEARCH_ACTION);
        actionSteps.put("浏览", UserActionLog.VIEW_ACTION);
        actionSteps.put("收藏", UserActionLog.FAVORITE_ACTION);
        actionSteps.put("预定", UserActionLog.CLICK_TRIP_ACTION);

        // 2. 构造返回结果：Echarts 漏斗图通常需要 [{name: '搜索', value: 100}, ...] 格式
        List<Map<String, Object>> funnelData = new ArrayList<>();

        for (Map.Entry<String, String> entry : actionSteps.entrySet()) {
            String label = entry.getKey();
            String actionType = entry.getValue();

            // 统计各阶段去重用户数 (DISTINCT user_id)
            // 如果想统计总行为次数，可以去掉 .select("DISTINCT user_id")
            Long count = userActionLogMapper.selectCount(new QueryWrapper<UserActionLog>()
                    .eq("action_type", actionType));

            Map<String, Object> dataItem = new HashMap<>();
            dataItem.put("name", label);
            dataItem.put("value", count);
            funnelData.add(dataItem);
        }

        return R.ok(funnelData);
    }


    //转换 Long

    private Long getLongValue(Object obj) {
        if (obj == null) return 0L;
        return ((Number) obj).longValue();
    }
    //格式化平均分，保留一位小数
    private Double formatAverage(Object val) {
        if (val == null) return 0.0;
        return Math.round(Double.parseDouble(val.toString()) * 10) / 10.0;
    }
}
