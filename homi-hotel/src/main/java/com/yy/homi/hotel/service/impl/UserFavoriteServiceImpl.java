package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}