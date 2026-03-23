package com.yy.homi.hotel.controller;

import com.yy.homi.hotel.service.HotelRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/hotelroom")
public class HotelRoomController {

    @Autowired
    private HotelRoomService hotelRoomService;



}
