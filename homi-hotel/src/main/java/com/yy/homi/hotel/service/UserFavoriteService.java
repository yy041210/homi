package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.UserFavorite;

public interface UserFavoriteService extends IService<UserFavorite> {

    /**
     * 切换收藏状态
     */
    R toggleFavorite(String userId, String hotelId);

    /**
     * 分页查询收藏列表
     */
    R getMyFavoriteList(String userId, int pageNum, int pageSize);

    /**
     * 检查收藏状态
     */
    R checkFavoriteStatus(String userId, String hotelId);
}