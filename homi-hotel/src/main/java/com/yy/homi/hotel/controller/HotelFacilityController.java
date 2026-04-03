package com.yy.homi.hotel.controller;

import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelFacilityPageListReqDTO;
import com.yy.homi.hotel.service.HotelFacilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;

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

    @GetMapping("/changeStatus")
    public R changeStatus(@RequestParam("id") @NotBlank(message = "设备id不能为空！") String id) {
        return hotelFacilityService.changeStatus(id);
    }

    @Operation(summary = "根据id删除酒店设备")
    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("id") @NotBlank(message = "设备id不能为空！") String id) {
        if (StrUtil.isBlank(id)) {
            return R.fail("设备id不能为空！");
        }
        hotelFacilityService.removeById(id);
        return R.ok("删除成功！");
    }

    @Operation(summary = "获取全量酒店设施筛选器")
    @GetMapping("/getHotelFacilityFilters")
    public R getHotelFacilityFilters() {
        return hotelFacilityService.getHotelFacilityFilters();
    }


}