package com.yy.homi.hotel.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.BusinessType;
import com.yy.homi.hotel.domain.convert.HotelSurroundingConverter;
import com.yy.homi.hotel.domain.dto.request.HotelSurroundingPageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelSurroundingUpdateReqDTO;
import com.yy.homi.hotel.domain.entity.HotelSurrounding;
import com.yy.homi.hotel.service.HotelSurroundingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/hotelsurrounding")
public class HotelSurroundingController {

    @Autowired
    private HotelSurroundingService hotelSurroundingService;
    @Autowired
    private HotelSurroundingConverter hotelSurroundingConverter;

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
    @AutoLog(title = "酒店周边列表-新增酒店周边",businessType = BusinessType.INSERT)
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

    @AutoLog(title = "酒店周边列表-根据id修改酒店周边",businessType = BusinessType.UPDATE)
    @PostMapping("/updateById")
    public R updateById(@Validated @RequestBody HotelSurroundingUpdateReqDTO reqDTO) {
        HotelSurrounding hotelSurrounding = hotelSurroundingConverter.updateDtoToEntity(reqDTO);
        hotelSurroundingService.updateById(hotelSurrounding);
        return R.ok("修改成功！");
    }

    @AutoLog(title = "酒店周边列表-根据id删除酒店周边",businessType = BusinessType.DELETE)
    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("id") @NotBlank(message = "周边id不能为空！") String id) {
        hotelSurroundingService.removeById(id);
        return R.ok("删除成功！");
    }

    @AutoLog(title = "酒店周边列表-根据ids批量删除酒店周边",businessType = BusinessType.DELETE)
    @PostMapping("/deleteByIds")
    public R deleteByIds(@RequestBody List<String> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return R.fail("周边ids集合不能为空！");
        }
        hotelSurroundingService.removeBatchByIds(ids);
        return R.ok("删除成功！");
    }

}