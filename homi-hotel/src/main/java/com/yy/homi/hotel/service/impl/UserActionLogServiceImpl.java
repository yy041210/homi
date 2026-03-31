package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.convert.UserActionLogConverter;
import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelStats;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import com.yy.homi.hotel.domain.vo.HotelVO;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelStatsMapper;
import com.yy.homi.hotel.mapper.UserActionLogMapper;
import com.yy.homi.hotel.service.UserActionLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            BeanUtils.copyProperties(hotelBase,hotelVO);
            String hotelId = hotelBase.getId();
            HotelStats stats = statsIdentityMap.get(hotelId);
            if(stats !=null){
                BeanUtils.copyProperties(stats,hotelVO);
            }

            if(hotelAlbumsIdMap.get(hotelId) != null){
                hotelVO.setPicUrls(hotelAlbumsIdMap.get(hotelId).stream().map(HotelAlbum::getImageUrl).collect(Collectors.toList()));
            }else{
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
            userActionLogJson.put("hotelVO",hotelVO);

            result.add(userActionLogJson);
        }

        return R.ok(result);

    }


    @Override
    public R countViewByUserId(String userId) {
        if(StrUtil.isBlank(userId)){
            return R.fail("用户id不能为空！");
        }

        Integer count =  userActionLogMapper.countViewByUserId(userId);

        return R.ok(count);
    }

}