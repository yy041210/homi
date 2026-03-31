package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelRank;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.UserActionLogMapper;
import com.yy.homi.hotel.service.AnalysisService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Autowired
    private UserActionLogMapper userActionLogMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;

    @Override
    public R hotRankList(String type, Integer pageSize) {
        if (StrUtil.isBlank(type) || pageSize == null) {
            return R.fail("参数错误！");
        }

        //total / hot / praise / favorite
        if(type.equals("total")){
//            List<HotelRank> totalRankList = getTotalRankList(pageSize);
            return R.ok();
        } else if (type.equals("hot")) {

        } else if (type.equals("praise")) {

        } else if (type.equals("favorite") ){

        }else{
            return R.fail("暂无该类型的排行榜！");
        }


        return null;
    }

//    /**
//     * 综合排行榜 - 基于酒店综合评分
//     */
//    private List<HotelRank> getTotalRankList(Integer pageSize) {
//
//        QueryWrapper<UserActionLog> wrapper = new QueryWrapper<>();
//        wrapper.select("hotel_id",
//                        "SUM(action_weight) as totalWeight")
//                .groupBy("hotel_id");
//
//        List<Map<String, Object>> actionWeightMaps = userActionLogMapper.selectMaps(wrapper);
//
//        //转换并按分数排序
//        List<HotelRank> sortedRanks = actionWeightMaps.stream()
//                .map(map -> {
//                    HotelRank rank = new HotelRank();
//                    rank.setHotelId(String.valueOf(map.get("hotel_id")));
//                    Object score = map.get("totalScore");
//                    return rank;
//                })
//                .sorted(Comparator.comparing(HotelRank::getTotalScore).reversed())
//                .collect(Collectors.toList());
//
//        Set<String> hotelIds = sortedRanks.stream().map(HotelRank::getHotelId).collect(Collectors.toSet());
//        List<HotelBase> hotelBases = hotelBaseMapper.selectBatchIds(hotelIds);
//        Map<String, HotelBase> hotelIdMap = CollStreamUtil.toIdentityMap(hotelBases, HotelBase::getId);
//
//        Map<String, List<String>> idImageUrlsMap = hotelAlbumMapper.selectTop5PhotosBatch(new ArrayList<>(hotelIds)).stream().collect(Collectors.groupingBy(
//                HotelAlbum::getHotelId,
//                Collectors.mapping(HotelAlbum::getImageUrl,
//                        Collectors.toList())
//        ));
//
//        //设置排名序号
//        for (int i = 0; i < sortedRanks.size(); i++) {
//            HotelRank hotelRank = sortedRanks.get(i);
//            hotelRank.setRank(i + 1); // 第一名为 1
//            String hotelId = hotelRank.getHotelId();
//            if(hotelIdMap.get(hotelId) == null){
//                continue;
//            }
//            BeanUtils.copyProperties(hotelIdMap.get(hotelId),hotelRank);
//            List<String> imageUrls = idImageUrlsMap.get(hotelId);
//            if(CollectionUtil.isNotEmpty(imageUrls)){
//                hotelRank.setPicUrl(imageUrls.get(0));
//            }
//
//        }
//
//        return sortedRanks;
//    }


}
