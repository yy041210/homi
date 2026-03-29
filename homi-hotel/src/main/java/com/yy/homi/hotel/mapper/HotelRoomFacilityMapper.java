package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.HotelRoomFacility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 房型设施 Mapper 接口
 */
@Mapper
public interface HotelRoomFacilityMapper extends BaseMapper<HotelRoomFacility> {
    // 如果需要复杂的统计查询，可以在此处定义 XML 方法

    @Select("SELECT name, COUNT(*) as count FROM hotel_room_facility GROUP BY name ORDER BY count DESC LIMIT 20")
    List<Map<String, Integer>> getTopRoomFacilityFilters();
}