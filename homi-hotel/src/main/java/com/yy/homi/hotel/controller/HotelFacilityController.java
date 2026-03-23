package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelFacilityPageListReqDTO;
import com.yy.homi.hotel.service.HotelFacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "酒店设施管理")
@RestController
@RequestMapping("/hotelfacility")
public class HotelFacilityController {

    @Resource
    private HotelFacilityService hotelFacilityService;

    @Operation(summary = "分页查询酒店设施")
    @PostMapping("/pageList")
    public R pageList(@RequestBody HotelFacilityPageListReqDTO reqDTO) {
        return hotelFacilityService.pageList(reqDTO);
    }
}