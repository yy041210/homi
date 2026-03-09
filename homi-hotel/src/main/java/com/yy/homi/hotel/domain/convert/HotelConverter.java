package com.yy.homi.hotel.domain.convert;

import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.vo.HotelVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HotelConverter {
    // 单个转换
    HotelVO entityToVo(HotelBase entity);

    // 集合转换
    List<HotelVO> listEntityToVo(List<HotelBase> list);
}