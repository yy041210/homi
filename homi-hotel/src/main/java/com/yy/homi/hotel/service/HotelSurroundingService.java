package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.HotelSurrounding;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HotelSurroundingService extends IService<HotelSurrounding> {
    /**
     * 根据酒店ID获取周边信息
     */
    List<HotelSurrounding> findByHotelId(String hotelId);
    
    /**
     * 根据分类获取周边
     */
    List<HotelSurrounding> findByHotelIdAndCategory(String hotelId, Integer category);

    R importHotelSurroundingFromCsv(MultipartFile file);
}