package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.*;

import com.yy.homi.hotel.domain.vo.HotelVO;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelStatsMapper;
import com.yy.homi.hotel.mapper.UserFavoriteMapper;
import com.yy.homi.hotel.service.UserFavoriteService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements UserFavoriteService {

    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;
    @Autowired
    private HotelStatsMapper hotelStatsMapper;
    @Autowired
    private UserFavoriteMapper userFavoriteMapper;


    @Override
    @Transactional
    public R toggleFavorite(String userId, String hotelId) {
        if (StrUtil.hasBlank(userId, hotelId)) {
            return R.fail("参数不能为空");
        }

        LambdaQueryWrapper<UserFavorite> query = new LambdaQueryWrapper<>();
        query.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getHotelId, hotelId);

        UserFavorite exist = this.getOne(query);

        if (exist != null) {
            // 逻辑：取消收藏
            this.removeById(exist.getId());
            return R.ok("取消收藏成功");
        } else {
            // 逻辑：新增收藏
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setHotelId(hotelId);
            favorite.setCreateTime(new Date()); //

            //保存到日志表


            this.save(favorite);
            return R.ok("收藏成功");
        }
    }

    @Override
    public R getMyFavoriteList(String userId, int pageNum, int pageSize) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("用户ID不能为空");
        }

        PageHelper.startPage(pageNum, pageSize);
        List<UserFavorite> list = this.lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreateTime) //
                .list();

        if (CollectionUtil.isEmpty(list)) {
            return R.ok(new ArrayList<>());
        }
        return R.ok(list);
    }

    @Override
    public R checkFavoriteStatus(String userId, String hotelId) {
        if (StrUtil.hasBlank(userId, hotelId)) {
            return R.fail("参数不足");
        }

        long count = this.lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getHotelId, hotelId)
                .count();

        return R.ok(count > 0);
    }

    @Override
    public R getFavorites(String userId, Integer pageNum, Integer pageSize) {
        if (StrUtil.isBlank("用户id不能为空！") || pageNum == null || pageSize == null) {
            return R.fail("请求参数错误！");
        }

        PageHelper.startPage(pageNum, pageSize);

        List<UserFavorite> userFavorites = userFavoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreateTime));

        Set<String> hotelIds = userFavorites.stream().map(UserFavorite::getHotelId).collect(Collectors.toSet());

        if (CollectionUtil.isEmpty(hotelIds)) {
            return R.ok(new ArrayList<>());
        }

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
        for (UserFavorite userFavorite : userFavorites) {
            String hotelId = userFavorite.getHotelId();
            HotelVO hotelVO = hotelVoIdentityMap.get(hotelId);
            if (hotelVO == null) {
                continue;
            }
            JSONObject userFavoriteJson = JSONObject.parseObject(JSONObject.toJSONString(userFavorite));
            userFavoriteJson.put("hotelVO", hotelVO);
            result.add(userFavoriteJson);
        }

        return R.ok(result);
    }

    @Override
    public R countFavoriteByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("userId不能为空！");
        }

        Integer count = userFavoriteMapper.countFavoriteByUserId(userId);
        return R.ok(count != null ? count : 0);
    }
}