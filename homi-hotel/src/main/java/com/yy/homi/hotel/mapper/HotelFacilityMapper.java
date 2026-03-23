package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.dto.FacilityTypeCountDTO;
import com.yy.homi.hotel.domain.entity.HotelFacility;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface HotelFacilityMapper extends BaseMapper<HotelFacility> {

    @Select("select hotel_facility_type_id,count(1) as count from hotel_facility group by hotel_facility_type_id")
    List<FacilityTypeCountDTO> countGroupByTypeId();

    @Select("select count(1) from hotel_facility where status = 1")
    int countDisabled();

    @Select("select count(1) from hotel_facility where status = 0")
    int countEnabled();

    @Update("update hotel_facility set status = #{newStatus} where id = #{id}")
    int changeStatus(@Param("id") String id,@Param("newStatus") Integer newStatus);
}
