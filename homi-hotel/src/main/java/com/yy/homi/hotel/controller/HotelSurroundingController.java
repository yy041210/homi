package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelSurroundingPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelSurrounding;
import com.yy.homi.hotel.service.HotelSurroundingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/hotelsurrounding")
public class HotelSurroundingController {

    @Autowired
    private HotelSurroundingService hotelSurroundingService;

    /**
     * 获取指定酒店的所有周边信息
     */
    @GetMapping("/list/{hotelId}")
    public List<HotelSurrounding> getList(@PathVariable String hotelId) {
        return hotelSurroundingService.findByHotelId(hotelId);
    }

    /**
     * 按分类筛选酒店周边（如只需交通/景点）
     */
    @GetMapping("/filter")
    public List<HotelSurrounding> filter(@RequestParam String hotelId, @RequestParam Integer category) {
        return hotelSurroundingService.findByHotelIdAndCategory(hotelId, category);
    }

    /**
     * 保存单条周边数据
     */
    @PostMapping("/save")
    public boolean save(@RequestBody HotelSurrounding surrounding) {
        return hotelSurroundingService.save(surrounding);
    }

    @PostMapping("/pageList")
    public R pageList(@RequestBody HotelSurroundingPageListReqDTO reqDTO) {
        return hotelSurroundingService.pageList(reqDTO);
    }

    @PostMapping("/importHotelSurroundingFromCsv")
    public R importHotelSurroundingFromCsv(@RequestParam("file") MultipartFile file) {
        return hotelSurroundingService.importHotelSurroundingFromCsv(file);
    }
}