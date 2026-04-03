package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelStats;
import com.yy.homi.hotel.domain.vo.HotelVO;
import com.yy.homi.hotel.feign.SysCityFeign;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelStatsMapper;
import com.yy.homi.hotel.service.RecommendService;
import org.elasticsearch.http.HttpStats;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelStatsMapper hotelStatsMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;

    @Autowired
    private SysCityFeign sysCityFeign;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public R recommendByTop10City() {
        //从redis中读取数据
        String jsonStr = redisTemplate.opsForValue().get("homi:hotel:city:recommend").toString();

        if(StrUtil.isEmpty(jsonStr)){
            return R.ok(new ArrayList<>());
        }

        Map<String, List<String>> redisResult = JSON.parseObject(jsonStr, Map.class);

        List<Integer> cityIds = redisResult.entrySet().stream().map(entry -> entry.getKey()).map(item -> StrUtil.isEmpty(item) ? null : Integer.parseInt(item)).collect(Collectors.toList());
        List<String> allHotelIds = redisResult.entrySet().stream().flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());

        R r = sysCityFeign.getNamesByIds(cityIds);
        if(r.getCode() != HttpStatus.OK.value()){
            return R.fail("远程调用查询市名失败！");
        }
        Map<String, String> cityIdNameMap = (Map<String, String>) r.getData();
        List<HotelBase> hotelBases = hotelBaseMapper.selectBatchIds(allHotelIds);
        Map<String, HotelBase> hotelIdMap = CollStreamUtil.toIdentityMap(hotelBases, HotelBase::getId);

        List<HotelStats> hotelStatsList = hotelStatsMapper.selectList(new LambdaQueryWrapper<HotelStats>().in(HotelStats::getHotelId, allHotelIds));
        Map<String, HotelStats> hotelStatsMap = CollStreamUtil.toIdentityMap(hotelStatsList, HotelStats::getHotelId);

        Map<String, List<HotelAlbum>> hotelIdImageUrlsMap = hotelAlbumMapper.selectTop5PhotosBatch(allHotelIds).stream().collect(Collectors.groupingBy(HotelAlbum::getHotelId));

        List<JSONObject> result = new ArrayList<>();
        for (Map.Entry<String,List<String>> entry : redisResult.entrySet()){
            JSONObject jsonObject = new JSONObject();
            String cityId = entry.getKey();
            String cityName = cityIdNameMap.get(cityId);
            jsonObject.put("cityId",cityId);
            jsonObject.put("cityName",cityName);
            List<String> hotelIds = entry.getValue();
            List<HotelVO> hotelVOS = hotelIds.stream().filter(hotelId -> hotelIdMap.get(hotelId) != null)
                    .map(hotelId -> {
                        HotelVO hotelVO = new HotelVO();
                        HotelBase hotelBase = hotelIdMap.get(hotelId);
                        BeanUtils.copyProperties(hotelBase, hotelVO);
                        HotelStats hotelStats = hotelStatsMap.get(hotelId);
                        if (hotelStats != null) {
                            BeanUtils.copyProperties(hotelStats, hotelVO);
                        }
                        List<String> imageUrls = hotelIdImageUrlsMap.get(hotelId) == null ? new ArrayList<>(): hotelIdImageUrlsMap.get(hotelId).stream().map(HotelAlbum::getImageUrl).collect(Collectors.toList());
                        hotelVO.setPicUrls(imageUrls);
                        return hotelVO;
                    }).collect(Collectors.toList());
            jsonObject.put("hotels",hotelVOS);

            result.add(jsonObject);

        }

        return R.ok(result);
    }
}
