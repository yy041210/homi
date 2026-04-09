package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.service.AnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    /**
     * 获取酒店星级分布饼状图数据
     */
    @GetMapping("/hotelStarDistribute")
    public R getHotelStarDistribute() {
        return analysisService.getHotelStarDistribute();
    }

    /**
     * 获取城市酒店数量统计
     */
    @GetMapping("/cityHotelCount")
    public R getCityHotelCount() {
        return analysisService.getCityHotelCount();
    }

    /**
     * 获取酒店开业年份分布统计
     */
    @GetMapping("/openYearDistribute")
    public R getOpenYearDistribute() {
        return analysisService.getOpenYearDistribute();
    }

    /**
     * 获取酒店综合评分分布统计
     */
    @GetMapping("/commentScoreDistribute")
    public R getCommentScoreDistribute() {
        return analysisService.getCommentScoreDistribute();
    }

    /**
     * 获取酒店各维度平均分（雷达图）
     */
    @GetMapping("/hotelRadarStats")
    public R getHotelRadarStats() {
        return analysisService.getHotelRadarStats();
    }

    /**
     * 获取酒店评论数量分布统计（柱状图）
     */
    @GetMapping("/commentCountDistribute")
    public R getCommentCountDistribute() {
        return analysisService.getCommentCountDistribute();
    }


    /**
     * 获取房型价格区间分布统计
     */
    @GetMapping("/roomPriceDistribute")
    public R getRoomPriceDistribute() {
        return analysisService.getRoomPriceDistribute();
    }

    /**
     * 获取房型床型分布统计
     */
    @GetMapping("/bedTypeDistribute")
    public R getBedTypeDistribute() {
        return analysisService.getBedTypeDistribute();
    }

    /**
     * 获取房型面积分布统计
     */
    @GetMapping("/roomAreaDistribute")
    public R getRoomAreaDistribute() {
        return analysisService.getRoomAreaDistribute();
    }

    /**
     * 获取房型价格与面积分布数据（气泡图）
     */
    @GetMapping("/roomBubbleStats")
    public R getRoomBubbleStats() {
        return analysisService.getRoomBubbleStats();
    }

    /**
     * 获取酒店房间设施词云图数据
     */
    @GetMapping("/roomFacilityWordCloud")
    public R getRoomFacilityWordCloud() {
        return analysisService.getRoomFacilityWordCloud();
    }

    /**
     * 获取酒店设施类型分布统计
     */
    @GetMapping("/facilityTypeDistribute")
    public R getFacilityTypeDistribute() {
        return analysisService.getFacilityTypeDistribute();
    }

    /**
     * 获取酒店整体设施词云图数据
     */
    @GetMapping("/hotelFacilityWordCloud")
    public R getHotelFacilityWordCloud() {
        return analysisService.getHotelFacilityWordCloud();
    }

    /**
     * 获取酒店周边距离分布统计\
     */
    @GetMapping("/surroundingDistanceDistribute")
    public R getSurroundingDistanceDistribute() {
        return analysisService.getSurroundingDistanceDistribute();
    }

    /**
     * 获取各星级酒店周边配套均数（分组柱状图）
     */
    @GetMapping("/starSurroundingAvg")
    public R getStarSurroundingAvg() {
        return analysisService.getStarSurroundingAvg();
    }

    /**
     * 获取24小时用户行为活跃度分布 (堆叠柱状图数据)
     */
    @GetMapping("/userActiveTime")
    public R getUserActiveTimeDistribution() {
        return analysisService.getUserActiveTimeDistribution();
    }

    /**
     * 获取用户对不同星级酒店的浏览偏好
     */
    @GetMapping("/userStarPreference")
    public R getUserStarPreference() {
        return analysisService.getUserStarPreference();
    }

    /**
     * 获取用户浏览量最高的 Top 10 城市
     */
    @GetMapping("/topCities")
    public R getTopBrowsedCities() {
        return analysisService.getTopBrowsedCities();
    }

    /**
     * 获取用户对不同价格区间的浏览偏好
     */
    @GetMapping("/userPricePreference")
    public R getUserPricePreference() {
        return analysisService.getUserPricePreference();
    }

    /**
     * 获取用户行为转化漏斗数据
     */
    @GetMapping("/userFunnel")
    public R getUserActionFunnel() {
        return analysisService.getUserActionFunnel();
    }

}
