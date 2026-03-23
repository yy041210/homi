package com.yy.homi.hotel.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelCommentPageListReqDTO;
import com.yy.homi.hotel.service.HotelCommentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Validated
@Slf4j
@RestController
@RequestMapping("/hotelcomment")
public class HotelCommentController {

    @Autowired
    private HotelCommentService hotelCommentService;

    /**
     * 分页查询酒店评论列表
     */
    @PostMapping("/pageList")
    public R pageList(@Validated @RequestBody HotelCommentPageListReqDTO reqDTO) {
        // 基础校验
        return hotelCommentService.pageList(reqDTO);
    }

    @Operation(summary = "根据id删除评论")
    @GetMapping("/deleteById")
    public R deleteById(@RequestParam("id") @NotBlank(message = "评论id不能为空！") String id) {
        if(StrUtil.isNotBlank(id)){
            hotelCommentService.removeById(id);
        }
        return R.ok("删除成功！");
    }

    @Operation(summary = "根据ids批量删除评论")
    @PostMapping("/deleteByIds")
    public R deleteByIds(@Validated @RequestBody @NotEmpty List<String> ids){
        if(CollectionUtil.isNotEmpty(ids)){
            hotelCommentService.removeByIds(ids);
        }
        return R.ok("删除成功！");
    }

}
