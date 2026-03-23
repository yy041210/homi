package com.yy.homi.hotel.domain.convert;

import com.yy.homi.hotel.domain.dto.request.HotelSurroundingUpdateReqDTO;
import com.yy.homi.hotel.domain.entity.HotelSurrounding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HotelSurroundingConverter {

    HotelSurrounding updateDtoToEntity(HotelSurroundingUpdateReqDTO hotelSurroundingUpdateReqDTO);
}