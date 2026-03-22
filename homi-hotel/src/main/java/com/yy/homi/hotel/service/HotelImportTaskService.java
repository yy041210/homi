package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelImportTaskPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HotelImportTaskService extends IService<HotelImportTask> {
    R insertHotelImportTask(String taskName, String taskType, MultipartFile file);

    R pageList(HotelImportTaskPageListReqDTO queryDTO);

    R getImportTaskByIds(List<String> ids);

}
