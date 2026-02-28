package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.service.HotelBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/hotelbase")
public class HotelBaseController {

    @Autowired
    private HotelBaseService hotelBaseService;


    @PostMapping("/importHotelBaseFromJsonCsv")
    public R importHotelBaseFromJsonCsv(@RequestParam("file")MultipartFile file){
        return hotelBaseService.importHotelBaseFromJsonCsv(file);
    }


}
