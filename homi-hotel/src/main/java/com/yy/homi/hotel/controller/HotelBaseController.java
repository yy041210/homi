package com.yy.homi.hotel.controller;

import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.BusinessType;
import com.yy.homi.hotel.domain.dto.request.HotelBasePageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelDocPageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelInsertDTO;
import com.yy.homi.hotel.service.HotelBaseService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;


@Validated
@RestController
@RequestMapping("/hotelbase")
public class HotelBaseController {

    @Autowired
    private HotelBaseService hotelBaseService;

    @GetMapping("/count")
    public R count(){
        return R.ok(hotelBaseService.count());
    }


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

    @Operation(summary = "根据id删除酒店详情（基本信息,设备，房型，图集，简介，评论，附近）")
    @AutoLog(title = "酒店列表-根据id删除酒店",businessType = BusinessType.DELETE)
    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("id") @NotBlank(message = "酒店id不能为空！") String id){
        return hotelBaseService.deleteById(id);
    }



    @AutoLog(title = "酒店列表-启用禁用酒店",businessType = BusinessType.UPDATE)
    @GetMapping("/changeStatus")
    public R changeStatus(@RequestParam("id") @NotBlank(message = "酒店id不能为空!") String id){
        return hotelBaseService.changeStatus(id);
    }

    @PostMapping("/searchPageList")
    public R searchPageList(@RequestBody HotelDocPageListReqDTO reqDTO){
        return hotelBaseService.searchPageList(reqDTO);
    }

    @GetMapping("/suggestion")
    public R suggestion(@RequestParam("key") @NotBlank(message = "关键词不能为空！") String key) throws IOException {
        return hotelBaseService.suggestion(key);
    }

    /**
     * 获取酒店及房型设施筛选器
     * 效果：根据当前搜索词、城市等条件，动态聚合出可用的设施标签
     */
    @GetMapping("/getHotelFacilityFilters")
    public R getHotelFacilityFilters() {
        return hotelBaseService.getHotelFacilityFilters();
    }

    @GetMapping("/syncHotelDocFromDB")
    public R syncHotelDocFromDB(){
        return hotelBaseService.syncHotelDocFromDB();
    }


    @GetMapping("/getRecommendHotelList")
    public R getRecommendHotelList(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId){
        return hotelBaseService.getRecommendHotelList(userId);
    }

    @GetMapping("/countHotelByCity")
    public R countHotelByCity(){
        return hotelBaseService.countHotelByCity();
    }

}
