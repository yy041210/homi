package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.HotelBase;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface HotelBaseService extends IService<HotelBase> {

    R importHotelBaseFromJsonCsv(MultipartFile file);
}
