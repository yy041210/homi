package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.service.UserActionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Slf4j
@Validated
@RestController
@RequestMapping("/useractionlog")
public class UserActionLogController {

    @Autowired
    private UserActionLogService userActionLogService; // 必须这样写

    @PostMapping("/insertLog")
    public R insertLog(@Validated @RequestBody UserActionLogInsertReqDTO reqDTO) {
        log.info("userActionLogService is: {}", userActionLogService); // 查看是否为 null
        return userActionLogService.insertLog(reqDTO);
    }

    @GetMapping("/getViewHistory")
    public R getViewHistory(@Validated @RequestParam("userId") @NotBlank(message = "userId不能为空！") String userId,
                            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return userActionLogService.getViewHistory(userId, pageNum, pageSize);
    }

    @GetMapping("/countViewByUserId")
    public R countViewByUserId(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId) {
        return userActionLogService.countViewByUserId(userId);
    }

    @GetMapping("/countViewHotelStarByUserId")
    public R countViewHotelStarByUserId(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId) {
        return userActionLogService.countViewHotelStarByUserId(userId);
    }

    @GetMapping("/countViewCityByUserId")
    public R countViewCityByUserId(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId) {
        return userActionLogService.countViewCityByUserId(userId);
    }

    @GetMapping("/countViewHourByUserId")
    public R countViewHourByUserId(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId) {
        return userActionLogService.countViewHourByUserId(userId);
    }

    @GetMapping("/countViewPriceRangeByUserId")
    public R countViewPriceRangeByUserId(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId) {
        return userActionLogService.countViewPriceRangeByUserId(userId);
    }

    @GetMapping("/countViewCommentScoreRange")
    public R countViewCommentScoreRange(@RequestParam(value = "userId", required = false) String userId) {
        return userActionLogService.countViewCommentScoreRange(userId);
    }

    @GetMapping("/countViewCommentCountRange")
    public R countViewCommentCountRange(@RequestParam(value = "userId", required = false) String userId) {
        return userActionLogService.countViewCommentCountRange(userId);
    }


    @GetMapping("/countActionType")
    public R countActionTypeByUserId(@RequestParam(value = "userId", required = false) String userId) {
        return userActionLogService.countActionType(userId);
    }

    /**
     * 评分区间 x 价格分布 (箱线图) 接口
     */
    @GetMapping("/priceBoxPlot")
    public R getPriceBoxPlot(@RequestParam(value = "userId", required = false) String userId) {
        return userActionLogService.getPriceBoxPlotData(userId);
    }

    /**
     * 获取用户偏好的酒店评分维度 (雷达图数据)
     */
    @GetMapping("/radarStats")
    public R getRadarStats(@RequestParam(value = "userId", required = false) String userId) {
        return userActionLogService.getRadarStats(userId);
    }

    /**
     * 获取用户偏好的酒店开业年份统计（柱状图）
     */
    @GetMapping("/openingYear")
    public R getUserOpeningYearStats(@RequestParam(value = "userId", required = false) String userId) {
        // 返回格式包含：年份区间(name) 和 对应的浏览次数(value)
        return userActionLogService.countOpeningYearPreference(userId);

    }

    @GetMapping("/roomScaleStats")
    public R getRoomScaleStats(@RequestParam(value = "userId", required = false) String userId) {

        return userActionLogService.getHotelRoomScaleStats(userId);
    }

    @GetMapping("/facilityCloud")
    public R getFacilityCloudStats(@RequestParam(value = "userId", required = false) String userId) {
        // 获取用户浏览过的酒店设施词频统计
        return userActionLogService.getHotelFacilityCloud(userId);

    }

    @GetMapping("/roomAreaStats")
    public R getRoomAreaStats(@RequestParam(value = "userId", required = false)  String userId) {
        // 获取用户偏好的房型面积分布数据
        return userActionLogService.getRoomAreaStats(userId);
    }


    @GetMapping("/totalTrend")
    public R getTrend(
            @RequestParam String userId,
            @RequestParam Long beginTime,
            @RequestParam Long endTime) {
        return userActionLogService.getTotalTrend(userId, beginTime, endTime);
    }


}
