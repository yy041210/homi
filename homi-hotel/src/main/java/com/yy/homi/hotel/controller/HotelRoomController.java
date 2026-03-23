package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelRoomPageListReqDTO;
import com.yy.homi.hotel.service.HotelRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/hotelroom")
public class HotelRoomController {

    @Autowired
    private HotelRoomService hotelRoomService;

    @PostMapping("/pageList")
    public R pageList(@Validated @RequestBody HotelRoomPageListReqDTO reqDTO) {
        return hotelRoomService.pageList(reqDTO);
    }

    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("id") @NotBlank(message = "房型id不能为空！") String id){
        return hotelRoomService.deleteById(id);
    }

    @PostMapping("/deleteByIds")
    public R deleteByIds(@Validated @RequestBody @NotNull(message = "房型ids集合不能为空！") List<String> ids){
        return hotelRoomService.deleteByIds(ids);
    }

}
