package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelBasePageListReqDTO;
import com.yy.homi.hotel.service.HotelBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
@RestController
@RequestMapping("/hotelbase")
public class HotelBaseController {

    @Autowired
    private HotelBaseService hotelBaseService;


    @PostMapping("/importHotelBaseFromJsonCsv")
    public R importHotelBaseFromJsonCsv(@RequestParam("file")MultipartFile file){
        return hotelBaseService.importHotelBaseFromJsonCsv(file);
    }

    /**
     * 分页查询酒店列表
     * 请求路径：/hotelbase/pageList
     */
    @PostMapping("/pageList")
    public R pageList(@RequestBody HotelBasePageListReqDTO reqDTO) {
        return hotelBaseService.selectHotelPage(reqDTO);
    }

    @GetMapping("/getByDistrictId")
    public R getByDistrictId(@RequestParam("districtId") @NotNull Integer districtId){
        return hotelBaseService.getByDistrictId(districtId);
    }

    @GetMapping("/getByCityId")
    public R getByCityId(@RequestParam("cityId") @NotNull Integer cityId){
        return hotelBaseService.getByCityId(cityId);
    }
    @GetMapping("/getByProvinceId")
    public R getByProvinceId(@RequestParam("provinceId") @NotNull Integer provinceId){
        return hotelBaseService.getByProvinceId(provinceId);
    }
}
