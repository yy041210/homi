package com.yy.homi.hotel.controller;

import com.yy.homi.hotel.service.HotelBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hotelbase")
public class HotelBaseController {

    @Autowired
    private HotelBaseService hotelBaseService;



}
