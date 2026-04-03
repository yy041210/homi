package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.yy.homi.common.constant.RabbitMqConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.SysUserCache;
import com.yy.homi.common.enums.hotel.AlbumCategoryEnum;
import com.yy.homi.common.enums.hotel.AlbumSourceEnum;
import com.yy.homi.hotel.domain.convert.UserActionLogConverter;
import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelStats;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import com.yy.homi.hotel.domain.vo.HotelVO;
import com.yy.homi.hotel.feign.SysCityFeign;
import com.yy.homi.hotel.feign.SysUserFeign;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelStatsMapper;
import com.yy.homi.hotel.mapper.UserActionLogMapper;
import com.yy.homi.hotel.service.UserActionLogService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserActionLogServiceImpl extends ServiceImpl<UserActionLogMapper, UserActionLog> implements UserActionLogService {

    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;
    @Autowired
    private HotelStatsMapper hotelStatsMapper;
    @Autowired
    private UserActionLogMapper userActionLogMapper;
    @Autowired
    private UserActionLogConverter userActionLogConverter;

    @Autowired
    private SysUserFeign sysUserFeign;
    @Autowired
    private SysCityFeign sysCityFeign;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Override
    public R insertLog(UserActionLogInsertReqDTO reqDTO) {


        String hotelId = reqDTO.getHotelId();
        String actionType = reqDTO.getActionType();
        if (StrUtil.isBlank(hotelId) || StrUtil.isBlank(actionType)) {
            return R.fail("酒店id或操作类型不能为空！");
        }

        UserActionLog userActionLog = userActionLogConverter.insertReqDtoToEntity(reqDTO);
        userActionLog.setActionWeight(UserActionLog.getWeightByType(userActionLog.getActionType()));
        userActionLogMapper.insert(userActionLog);
        rabbitTemplate.convertAndSend(RabbitMqConstants.USER_ACTION_LOG_EXCHANGE, RabbitMqConstants.USER_ACTION_LOG_ROUTING_KEY, userActionLog);

        return R.ok("插入成功！");
    }

    @Override
    public R getViewHistory(String userId, Integer pageNum, Integer pageSize) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("用户id不能为空！");
        }

        // 1. 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 2. 构造查询条件
        LambdaQueryWrapper<UserActionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserActionLog::getUserId, userId)
                .eq(UserActionLog::getActionType, UserActionLog.VIEW_ACTION)
                // 核心：筛选 ID 属于“每个酒店最新一条记录”的集合
                // 使用 apply 嵌入子查询，{0} 会自动处理 SQL 注入防护
                .apply("id IN (SELECT t.max_id FROM (SELECT MAX(id) as max_id FROM user_action_log " +
                        "WHERE user_id = {0} AND action_type = {1} " +
                        "GROUP BY hotel_id) t)", userId, UserActionLog.VIEW_ACTION)
                // 排序：按创建时间倒序
                .orderByDesc(UserActionLog::getCreateTime);

        // 3. 执行查询
        List<UserActionLog> userActionLogs = userActionLogMapper.selectList(wrapper);

        if (CollectionUtil.isEmpty(userActionLogs)) {
            return R.ok(new ArrayList<>());
        }

        Set<String> hotelIds = userActionLogs.stream().map(UserActionLog::getHotelId).collect(Collectors.toSet());

        List<HotelBase> hotelBases = hotelBaseMapper.selectBatchIds(hotelIds);

        List<HotelStats> hotelStats = hotelStatsMapper.selectList(new LambdaQueryWrapper<HotelStats>().in(HotelStats::getHotelId, hotelIds));
        Map<String, HotelStats> statsIdentityMap = CollStreamUtil.toIdentityMap(hotelStats, HotelStats::getHotelId);

        Map<String, List<HotelAlbum>> hotelAlbumsIdMap = hotelAlbumMapper.selectTop5PhotosBatch(new ArrayList<>(hotelIds)).stream().collect(Collectors.groupingBy(
                HotelAlbum::getHotelId,
                Collectors.toList()
        ));

        List<HotelVO> hotelVOS = new ArrayList<>();
        for (HotelBase hotelBase : hotelBases) {
            HotelVO hotelVO = new HotelVO();
            BeanUtils.copyProperties(hotelBase, hotelVO);
            String hotelId = hotelBase.getId();
            HotelStats stats = statsIdentityMap.get(hotelId);
            if (stats != null) {
                BeanUtils.copyProperties(stats, hotelVO);
            }

            if (hotelAlbumsIdMap.get(hotelId) != null) {
                hotelVO.setPicUrls(hotelAlbumsIdMap.get(hotelId).stream().map(HotelAlbum::getImageUrl).collect(Collectors.toList()));
            } else {
                hotelVO.setPicUrls(new ArrayList<>());
            }

            hotelVOS.add(hotelVO);
        }

        Map<String, HotelVO> hotelVoIdentityMap = CollStreamUtil.toIdentityMap(hotelVOS, HotelVO::getId);
        List<JSONObject> result = new ArrayList<>();
        for (UserActionLog userActionLog : userActionLogs) {
            String hotelId = userActionLog.getHotelId();
            HotelVO hotelVO = hotelVoIdentityMap.get(hotelId);
            if (hotelVO == null) {
                continue;
            }

            JSONObject userActionLogJson = JSON.parseObject(JSON.toJSONString(userActionLog));
            userActionLogJson.put("hotelVO", hotelVO);

            result.add(userActionLogJson);
        }

        return R.ok(result);

    }


    @Override
    public R countViewByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("用户id不能为空！");
        }

        Integer count = userActionLogMapper.countViewByUserId(userId);

        return R.ok(count);
    }

    @Override
    public R countViewHotelStarByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("用户id不能为空！");
        }

        //查询所有浏览记录，根据酒店星级分组
        List<Map<String, Object>> countHotelStarMaps = userActionLogMapper.countViewHotelStarByUserId(userId);
        HashMap<String, Integer> countHotelStarMap = new HashMap<>();

        for (Map<String, Object> hotelStarMap : countHotelStarMaps) {
            Integer star = (Integer) hotelStarMap.get("star");
            Integer count = Integer.parseInt(hotelStarMap.get("count").toString());
            if (count == null || count == 0) {
                continue;
            }
            if (star == 1) {
                countHotelStarMap.put("一星", count);
            } else if (star == 2) {
                countHotelStarMap.put("二星", count);
            } else if (star == 3) {
                countHotelStarMap.put("三星", count);
            } else if (star == 4) {
                countHotelStarMap.put("四星", count);
            } else if (star == 5) {
                countHotelStarMap.put("五星", count);
            } else {
                Integer other = countHotelStarMap.get("其他");
                if (other == null) {
                    countHotelStarMap.put("其他", count);
                } else {
                    countHotelStarMap.put("其他", count + other);
                }
            }
        }

        return R.ok(countHotelStarMap);
    }

    @Override
    public R countViewCityByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("用户id不能为空！");
        }

        //查询所有浏览记录，根据酒店星级分组
        List<Map<String, Object>> countCityMaps = userActionLogMapper.countViewCityByUserId(userId);
        HashMap<String, Integer> countCityMap = new HashMap<>();
        List<Integer> cityIds = new ArrayList<>();

        for (Map<String, Object> cityMap : countCityMaps) {
            Integer cityId = Integer.valueOf(cityMap.get("city_id").toString());
            cityIds.add(cityId);
            Integer count = Integer.valueOf(cityMap.get("count").toString());
            countCityMap.put(cityId.toString(), count);
        }

        R r = sysCityFeign.getNamesByIds(cityIds);
        if (r.getCode() != HttpStatus.OK.value()) {
            return R.fail("远程调用查询市名失败！");
        }

        Map<String, String> cityIdNameMap = (Map<String, String>) r.getData();

        TreeMap<String, Integer> result = new TreeMap<>();

        for (Map.Entry<String, Integer> entry : countCityMap.entrySet()) {
            String cityId = entry.getKey();
            String cityName = cityIdNameMap.get(cityId);
            if (StrUtil.isBlank(cityName)) {
                continue;
            }
            result.put(cityName, entry.getValue());
        }


        return R.ok(result);
    }

    @Override
    public R countViewHourByUserId(String userId) {

        if (StrUtil.isBlank(userId)) {
            return R.fail("用户id不能为空！");
        }

        // 1. 从数据库查出已有的数据 [ {hour: 0, count: 1}, {hour: 5, count: 2} ... ]
        List<Map<String, Object>> list = userActionLogMapper.countViewByHour(userId);

        // 2. 转为 Map 方便提取
        Map<Integer, Integer> hourMap = list.stream().collect(Collectors.toMap(
                m -> ((Number) m.get("hour")).intValue(),
                m -> ((Number) m.get("count")).intValue()
        ));

        // 3. 补全 0-23 小时，如果没有数据则填充 0
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            result.add(hourMap.getOrDefault(i, 0));
        }
        return R.ok(result);
    }

    @Override
    public R countViewPriceRangeByUserId(String userId) {
        List<Map<String, Object>> rawData = userActionLogMapper.countPriceRangePreference(userId);

        // 定义标准顺序，确保饼图图例顺序固定
        List<String> standardOrder = Arrays.asList("<¥100", "¥100-200", "¥200-350", "¥350-500", "¥500-800", ">¥800");

        // 转为 Map 方便索引
        Map<String, Object> dataMap = rawData.stream()
                .collect(Collectors.toMap(m -> (String) m.get("name"), m -> m.get("value")));

        // 按顺序重新组装
        List<Map<String, Object>> result = standardOrder.stream().map(range -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", range);
            item.put("value", dataMap.getOrDefault(range, 0));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R countViewCommentScoreRange(String userId) {
        List<Map<String, Object>> rawData = userActionLogMapper.countViewCommentScoreRange(userId);
        // 定义标准顺序，确保饼图图例顺序固定
        List<String> standardOrder = Arrays.asList("0.0-3.0", "3.0-3.5", "3.5-4.0", "4.0-4.5", "4.5-4.8", "4.8-5.0");

        // 转为 Map 方便索引
        Map<String, Object> dataMap = rawData.stream()
                .collect(Collectors.toMap(m -> (String) m.get("name"), m -> m.get("value")));

        // 按顺序重新组装
        List<Map<String, Object>> result = standardOrder.stream().map(range -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", range);
            item.put("value", dataMap.getOrDefault(range, 0));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R countViewCommentCountRange(String userId) {
        List<Map<String, Object>> rawData = userActionLogMapper.countViewCommentCountRange(userId);
        // 定义标准顺序，确保饼图图例顺序固定
        List<String> standardOrder = Arrays.asList("0-500", "500-1000", "1000-2000", "2000-3000", "3000-4000", "4000-5000", "5000-7000", "7000-10000", ">=10000");

        // 转为 Map 方便索引
        Map<String, Object> dataMap = rawData.stream()
                .collect(Collectors.toMap(m -> (String) m.get("name"), m -> m.get("value")));

        // 按顺序重新组装
        List<Map<String, Object>> result = standardOrder.stream().map(range -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", range);
            item.put("value", dataMap.getOrDefault(range, 0));
            return item;
        }).collect(Collectors.toList());

        return R.ok(result);
    }


    @Override
    public R getRadarStats(String userId) {
        // 1. 调用数据层获取五个维度的平均分
        Map<String, Object> scores = userActionLogMapper.getUserRadarStats(userId);

        if (scores == null || scores.isEmpty()) {
            return null;
        }

        // 2. 提取并处理数据
        // 前四项（卫生、环境、设施、服务）保留一位小数
        double hygiene = formatScore(scores.get("hygiene"), 1);
        double environment = formatScore(scores.get("environment"), 1);
        double facility = formatScore(scores.get("facility"), 1);
        double service = formatScore(scores.get("service"), 1);

        // 3. 星级 (Star) 进行四舍五入取整
        long star = Math.round(Double.parseDouble(scores.get("star").toString()));

        // 4. 封装返回结果，对应雷达图的五个角
        JSONObject result = new JSONObject();
        result.put("indicator", Arrays.asList("卫生", "环境", "设施", "服务", "星级"));
        result.put("data", Arrays.asList(hygiene, environment, facility, service, (double) star));

        return R.ok(result);
    }

    @Override
    public R countOpeningYearPreference(String userId) {
        List<Map<String, Object>> openYearMaps = userActionLogMapper.countOpenYear(userId);
        Map<Integer, Integer> result = new HashMap<>();
        for (Map<String, Object> openYearMap : openYearMaps) {
            Integer openYear = Integer.valueOf(openYearMap.get("open_year").toString());
            Integer count = Integer.valueOf(openYearMap.get("count").toString());
            result.put(openYear, count);
        }
        return R.ok(result);
    }

    @Override
    public R getHotelRoomScaleStats(String userId) {
        // 1. 从 Mapper 获取酒店房间数分布原始数据
        List<Map<String, Object>> rawData = userActionLogMapper.selectRoomCountByUserId(userId);

        // 2. 初始化各区间计数器
        int less50 = 0;
        int range50to100 = 0;
        int range100to150 = 0;
        int range150to200 = 0;
        int more200 = 0;

        // 3. 遍历并分类统计
        for (Map<String, Object> item : rawData) {
            int count = Integer.parseInt(item.get("room_count").toString());
            int frequency = Integer.parseInt(item.get("frequency").toString());

            if (count < 50) less50 += frequency;
            else if (count <= 100) range50to100 += frequency;
            else if (count <= 150) range100to150 += frequency;
            else if (count <= 200) range150to200 += frequency;
            else more200 += frequency;
        }

        // 4. 组装成 ECharts 所需的格式 (name-value 键值对)
        List<Map<String, Object>> seriesData = new ArrayList<>();
        seriesData.add(createDataMap("<50间", less50));
        seriesData.add(createDataMap("50-100间", range50to100));
        seriesData.add(createDataMap("100-150间", range100to150));
        seriesData.add(createDataMap("150-200间", range150to200));
        seriesData.add(createDataMap(">200间", more200));

        Map<String, Object> result = new HashMap<>();
        result.put("series", seriesData);
        return R.ok(result);
    }

    @Override
    public R getHotelFacilityCloud(String userId) {
        // 1. 从数据库查询该用户浏览过的所有酒店对应的设施名称列表
        // SQL 关联 user_action_log 和 hotel_facility
        List<String> facilityNames = userActionLogMapper.selectFacilityNamesByUserId(userId);

        if (CollectionUtil.isEmpty(facilityNames)) {
            return R.ok(new ArrayList<>());
        }

        // 2. 使用 Stream API 进行词频统计
        Map<String, Long> counts = facilityNames.stream()
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

        // 3. 转换为前端词云图需要的格式 [{name: 'xxx', value: 10}, ...]
        List<Map<String, Object>> result = counts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", entry.getKey());
                    map.put("value", entry.getValue());
                    return map;
                })
                // 按频率降序排列，取前 30-50 个，避免词云太乱
                .sorted((a, b) -> ((Long) b.get("value")).compareTo((Long) a.get("value")))
                .limit(50)
                .collect(Collectors.toList());

        return R.ok(result);
    }

    @Override
    public R getRoomAreaStats(String userId) {
        // 1. 获取该用户所有相关的行为日志
        List<UserActionLog> logs = userActionLogMapper.selectAreaAndActionList(userId);

        // 2. 定义区间名称
        List<String> categories = Arrays.asList("<20㎡", "20-30㎡", "30-45㎡", "45-60㎡", ">60㎡");

        // 3. 初始化两个系列的计数数组（对应浏览和收藏）
        int[] viewData = new int[5];
        int[] favoriteData = new int[5];

        for (UserActionLog log : logs) {
            if (log.getMinArea() == null || log.getMaxArea() == null) continue;

            double avgArea = (log.getMinArea() + log.getMaxArea()) / 2.0;
            int index;

            // 判断所属区间索引
            if (avgArea < 20) index = 0;
            else if (avgArea <= 30) index = 1;
            else if (avgArea <= 45) index = 2;
            else if (avgArea <= 60) index = 3;
            else index = 4;

            // 根据行为类型增加对应计数值
            if (UserActionLog.VIEW_ACTION.equals(log.getActionType())) {
                viewData[index]++;
            } else if (UserActionLog.CLICK_TRIP_ACTION.equals(log.getActionType())) {
                favoriteData[index]++;
            }
        }

        // 4. 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);

        List<Map<String, Object>> series = new ArrayList<>();
        series.add(createSeriesMap("浏览次数", viewData));
        series.add(createSeriesMap("预定次数", favoriteData));
        result.put("series", series);

        return R.ok(result);
    }

    @Override
    public R getLatestOne() {
        UserActionLog userActionLog = userActionLogMapper.selectOne(
                new LambdaQueryWrapper<UserActionLog>()
                        .ne(UserActionLog::getActionType, UserActionLog.SEARCH_ACTION)
                        .orderByDesc(UserActionLog::getCreateTime)
                        .last("limit 1")
        );

        if (userActionLog == null) {
            return R.fail("暂无记录！");
        }
        String hotelId = userActionLog.getHotelId();
        if (StrUtil.isBlank(hotelId)) {
            log.error("日志酒店id为空！");
            return R.fail("系统异常！");
        }
        HotelBase hotelBase = hotelBaseMapper.selectById(hotelId);
        if (hotelBase == null) {
            log.error("日志hotelId对应酒店不存在！");
            return R.fail("系统异常！");
        }

        String userId = userActionLog.getUserId();
        if (StrUtil.isBlank(userId)) {
            log.error("日志用户id为空！");
            return R.fail("系统异常！");
        }

        R r = sysUserFeign.getUserInfo(userId);
        if (r.getCode() != HttpStatus.OK.value()) {
            return R.fail("远程调用rbac模块查询用户详情失败！");
        }

        if (r.getData() == null) {
            log.error("日志userId对应用户不存在！");
            return R.fail("系统异常！");
        }
        SysUserCache sysUserCache = JSON.parseObject(JSON.toJSONString(r.getData()), SysUserCache.class);

        HotelAlbum hotelAlbum = hotelAlbumMapper.selectOne(new LambdaQueryWrapper<HotelAlbum>()
                .eq(HotelAlbum::getHotelId, hotelId)
                .eq(HotelAlbum::getSource, AlbumSourceEnum.HOTEL.getCode())
                .eq(HotelAlbum::getCategory, AlbumCategoryEnum.FEATURED.getCode())
                .orderByAsc(HotelAlbum::getSeq)
                .last("limit 1")
        );

        JSONObject result = new JSONObject();
        result.put("userId", userId);
        result.put("nickName", sysUserCache.getNickName());
        result.put("hotelId", hotelId);
        result.put("hotelName", hotelBase.getName());
        if (hotelAlbum != null) result.put("imageUrl", hotelAlbum.getImageUrl());
        result.put("actionType", userActionLog.getActionType());
        result.put("createTime", userActionLog.getCreateTime());

        return R.ok(result);

    }

    private Map<String, Object> createSeriesMap(String name, int[] data) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("data", data);
        map.put("stack", "total"); // 开启堆叠的关键属性
        return map;
    }


    @Override
    public R getPriceBoxPlotData(String userId) {
        List<JSONObject> result = new ArrayList<>();

        // 对应图片 image_9bd00f.png 中的评分区间
        double[][] ranges = {{4.0, 4.5}, {4.5, 4.8}, {4.8, 5.1}}; // 5.1是为了包含5.0
        String[] labels = {"4.0-4.5", "4.5-4.8", "4.8-5.0"};

        for (int i = 0; i < ranges.length; i++) {
            List<Double> prices = userActionLogMapper.getPricesByScoreRange(ranges[i][0], ranges[i][1], userId);

            JSONObject box = new JSONObject();
            box.put("name", labels[i]);

            if (prices == null || prices.isEmpty()) {
                box.put("data", Arrays.asList(0, 0, 0, 0, 0));
                box.put("sampleSize", 0);
            } else {
                // 1. 必须排序才能计算分位数
                Collections.sort(prices);
                int size = prices.size();

                // 2. 计算 5 个核心点 (Min, Q1, Median, Q3, Max)
                double min = prices.get(0);
                double q1 = prices.get(size / 4);
                double median = prices.get(size / 2);
                double q3 = prices.get(size * 3 / 4);
                double max = prices.get(size - 1);

                // 3. 封装数据
                box.put("data", Arrays.asList(min, q1, median, q3, max));
                box.put("sampleSize", size); // 对应图中：样本 142家
            }
            result.add(box);
        }
        return R.ok(result);
    }


    @Override
    public R getTotalTrend(String userId, Long beginTime, Long endTime) {

        if (beginTime == null || endTime == null) {
            return R.fail("参数错误！");
        }

        Date startDate = new Date(beginTime);
        Date endDate = new Date(endTime);

        // 1. 从数据库获取原始数据
        List<Map<String, Object>> dbList = userActionLogMapper.countTrendByDate(userId, startDate, endDate);

        // 2. 将 DB 数据转为 Map 方便索引: Map<"2026-03-25", MapData>
        Map<String, Map<String, Object>> dbMap = dbList.stream()
                .collect(Collectors.toMap(m -> m.get("dateStr").toString(), m -> m));

        // 3. 计算时间差，准备循环补全
        long days = ChronoUnit.DAYS.between(
                startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );

        List<String> dates = new ArrayList<>();
        List<Long> viewData = new ArrayList<>();
        List<Long> favoriteData = new ArrayList<>();
        List<Long> searchData = new ArrayList<>();
        List<Long> bookingData = new ArrayList<>();

        // 4. 循环每一天，如果没有数据则填 0
        LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        for (int i = 0; i <= days; i++) {
            String dateStr = start.plusDays(i).toString();
            dates.add(dateStr);

            Map<String, Object> dayData = dbMap.get(dateStr);
            if (dayData != null) {
                viewData.add(((Number) dayData.get("viewCount")).longValue());
                favoriteData.add(((Number) dayData.get("favoriteCount")).longValue());
                searchData.add(((Number) dayData.get("searchCount")).longValue());
                bookingData.add(((Number) dayData.get("bookingCount")).longValue());
            } else {
                viewData.add(0L);
                favoriteData.add(0L);
                searchData.add(0L);
                bookingData.add(0L);
            }
        }

        // 5. 组装结果
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("viewData", viewData);
        result.put("favoriteData", favoriteData);
        result.put("searchData", searchData);
        result.put("bookingData", bookingData);
        return R.ok(result);
    }

    @Override
    public R countActionType(String userId) {

        //查询所有浏览记录，根据酒店星级分组
        List<Map<String, Object>> countActionTypeMaps = userActionLogMapper.countActionTypeByUserId(userId);
        HashMap<String, Integer> countActionTypeMap = new HashMap<>();

        for (Map<String, Object> countActionTypeMapDb : countActionTypeMaps) {
            String actionType = countActionTypeMapDb.get("action_type").toString();
            Integer count = Integer.valueOf(countActionTypeMapDb.get("count").toString());
            if (actionType.equals(UserActionLog.VIEW_ACTION)) {
                countActionTypeMap.put("浏览详情", count);
            } else if (actionType.equals(UserActionLog.SEARCH_ACTION)) {
                countActionTypeMap.put("搜索行为", count);
            } else if (actionType.equals(UserActionLog.FAVORITE_ACTION)) {
                countActionTypeMap.put("收藏酒店", count);
            } else if (actionType.equals(UserActionLog.CLICK_TRIP_ACTION)) {
                countActionTypeMap.put("预定酒店", count);
            } else {
                Integer other = countActionTypeMap.get("其他");
                if (other == null) {
                    countActionTypeMap.put("其他", count);
                } else {
                    countActionTypeMap.put("其他", other + count);
                }
            }
        }
        return R.ok(countActionTypeMap);
    }

    /**
     * 格式化数值
     *
     * @param obj   原始数据
     * @param scale 保留小数位数 (0 表示取整)
     */
    private double formatScore(Object obj, int scale) {
        if (obj == null) return 0.0;
        double val = Double.parseDouble(obj.toString());
        double factor = Math.pow(10, scale);
        return Math.round(val * factor) / factor;
    }

    private Map<String, Object> createDataMap(String name, int value) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }

}