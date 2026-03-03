package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.hotel.domain.entity.HotelFacilityType;

import java.util.List;

/**
 * 酒店设施类型 Service 接口
 */
public interface HotelFacilityTypeService extends IService<HotelFacilityType> {

    /**
     * 查询所有启用的设施类型
     * @return 设施类型列表
     */
    List<HotelFacilityType> findAllEnabled();
}