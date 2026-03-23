package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.HotelFacilityType;
import com.yy.homi.hotel.service.HotelFacilityTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/hotelfacilitytype")
public class HotelFacilityTypeController {

    @Autowired
    private HotelFacilityTypeService hotelFacilityTypeService;

    @GetMapping("/listAll")
    public R listAll(){
        return hotelFacilityTypeService.listAll();
    }

}
