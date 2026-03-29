package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.service.HotelRoomFacilityService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/hotelroomfacility")
public class HotelRoomFacilityController {

    @Autowired
    private HotelRoomFacilityService hotelRoomFacilityService;

    @Operation(summary = "获取全量房型设施筛选器")
    @GetMapping("/getHotelRoomFacilityFilters")
    public R getHotelRoomFacilityFilters() {
        return hotelRoomFacilityService.getHotelRoomFacilityFilters();
    }

}
