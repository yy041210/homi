package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.service.HotelImportTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.constraints.NotBlank;
@Validated
@RestController
@RequestMapping("/hotelimporttask")
public class HotelImportTaskController {

    @Autowired
    private HotelImportTaskService hotelImportTaskService;

    /**
     * 新建导入任务（异步执行 前端轮询查询处理结果）
     * @return 任务ID
     */
    @PostMapping("/insertHotelImportTask")
    public R insertHotelImportTask(@RequestParam("taskName") String taskName, @RequestParam("taskType") @NotBlank String taskType, @RequestParam("file") MultipartFile file) {
        return hotelImportTaskService.insertHotelImportTask(taskName,taskType,file);
    }

}
