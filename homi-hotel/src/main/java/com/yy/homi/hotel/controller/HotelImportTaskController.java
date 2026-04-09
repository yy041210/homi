package com.yy.homi.hotel.controller;

import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.annotation.AutoLog;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.BusinessType;
import com.yy.homi.hotel.domain.dto.request.HotelImportTaskPageListReqDTO;
import com.yy.homi.hotel.service.HotelImportTaskService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Validated
@RestController
@RequestMapping("/hotelimporttask")
public class HotelImportTaskController {

    @Autowired
    private HotelImportTaskService hotelImportTaskService;

    /**
     * 新建导入任务（异步执行 前端轮询查询处理结果）
     *
     * @return 任务ID
     */
    @AutoLog(title = "数据导入任务管理-新建导入任务",businessType = BusinessType.INSERT)
    @PostMapping("/insertHotelImportTask")
    public R insertHotelImportTask(@RequestParam("taskName") String taskName, @RequestParam("taskType") @NotBlank String taskType, @RequestParam("file") MultipartFile file) {
        return hotelImportTaskService.insertHotelImportTask(taskName, taskType, file);
    }

    //分页查询所有任务
    @Operation(summary = "分页查询任务列表")
    @PostMapping("/pageList")
    public R pageList(@RequestBody HotelImportTaskPageListReqDTO queryDTO) {
        // Service 返回具体的 Page<HotelImportTaskVO>
        return hotelImportTaskService.pageList(queryDTO);
    }

    @Operation(summary = "批量获取任务详情")
    @PostMapping("/getImportTaskByIds")
    public R getImportTaskByIds(@Validated @RequestBody @NotEmpty(message = "ids不能为空") List<String> ids) {
        // 直接透传 Service 返回的 R
        return hotelImportTaskService.getImportTaskByIds(ids);
    }

    @Operation(summary = "根据ids批量删除任务")
    @AutoLog(title = "数据导入任务管理-根据ids批量删除任务",businessType = BusinessType.DELETE)
    @PostMapping("/deleteByIds")
    public R deleteByIds(@Validated @RequestBody @NotEmpty(message = "ids不能为空") List<String> ids ){
        hotelImportTaskService.removeBatchByIds(ids);
        return R.ok();
    }

    @Operation(summary = "根据id删除任务")
    @AutoLog(title = "数据导入任务管理-根据id删除任务",businessType = BusinessType.DELETE)
    @GetMapping("/deleteById")
    public R deleteByIds(@RequestParam("id") @NotBlank(message = "任务id不能为空！") String id){
        if(StrUtil.isNotBlank(id)){
            hotelImportTaskService.removeById(id);
        }
        return R.ok("删除成功！");
    }

}
