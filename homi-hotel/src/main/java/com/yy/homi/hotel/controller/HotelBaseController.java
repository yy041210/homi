package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelBasePageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelInsertDTO;
import com.yy.homi.hotel.service.HotelBaseService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Validated
@RestController
@RequestMapping("/hotelbase")
public class HotelBaseController {

    @Autowired
    private HotelBaseService hotelBaseService;


    @PostMapping("/importHotelBaseFromJsonCsv")
    public R importHotelBaseFromJsonCsv(@RequestParam("file") MultipartFile file) {
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
    public R getByDistrictId(@RequestParam("districtId") @NotNull Integer districtId) {
        return hotelBaseService.getByDistrictId(districtId);
    }

    @GetMapping("/getByCityId")
    public R getByCityId(@RequestParam("cityId") @NotNull Integer cityId) {
        return hotelBaseService.getByCityId(cityId);
    }

    @GetMapping("/getByProvinceId")
    public R getByProvinceId(@RequestParam("provinceId") @NotNull Integer provinceId) {
        return hotelBaseService.getByProvinceId(provinceId);
    }

    @PostMapping("/saveHotel")
    public R saveHotel(@RequestBody HotelInsertDTO hotelInsertDTO) {
        // 调用业务层保存酒店完整信息
        return hotelBaseService.saveHotel(hotelInsertDTO);
    }

    @Operation(summary = "查询酒店详情（基本信息,设备，房型，图集，简介，评论，附近）")
    @GetMapping("/getInfoById")
    public R getInfoById(@RequestParam("id") @NotBlank(message = "酒店id不能为空！") String id){
        return hotelBaseService.getInfoById(id);
    }

    @GetMapping("/changeStatus")
    public R changeStatus(@RequestParam("id") @NotBlank(message = "酒店id不能为空!") String id){
        return hotelBaseService.changeStatus(id);
    }

}
