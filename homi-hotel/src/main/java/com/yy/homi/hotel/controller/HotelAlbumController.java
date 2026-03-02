package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.service.HotelAlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 酒店相册控制器
 * 提供酒店相册数据的导入功能
 */
@Slf4j
@RestController
@RequestMapping("/hotelalbum")
@Tag(name = "酒店相册管理", description = "酒店相册相关接口，支持CSV数据导入等功能")
public class HotelAlbumController {

    @Autowired
    private HotelAlbumService hotelAlbumService;

    /**
     * 从CSV文件导入酒店相册数据
     *
     * @param file CSV文件
     * @return 导入结果
     */
    @PostMapping(value = "/importHotelAlbumFromCsv")
    @Operation(summary = "从CSV文件导入酒店相册数据")
    public R importHotelAlbumFromCsv(@RequestParam("file") MultipartFile file) {
        return hotelAlbumService.importHotelAlbumFromCsv(file);
    }
}