package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.HotelBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface HotelBaseMapper extends BaseMapper<HotelBase> {

    /**
     * 根据字段条件查询酒店基本信息列表（单表查询）
     */
    List<HotelBase> selectHotelList(
            @Param("name") String name,
            @Param("star") Integer star,
            @Param("status") Integer status,
            @Param("provinceId") Integer provinceId,
            @Param("cityId") Integer cityId,
            @Param("districtId") Integer districtId,
            @Param("beginTime") Date beginTime,
            @Param("endTime") Date endTime
    );
}
