package com.yy.homi.hotel.domain.convert;

import com.yy.homi.hotel.domain.dto.request.HotelFacilityUpdateReqDTO;
import com.yy.homi.hotel.domain.entity.HotelFacility;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HotelFacilityConverter {
    HotelFacility updateReqDtoToEntity(HotelFacilityUpdateReqDTO reqDTO);
}
