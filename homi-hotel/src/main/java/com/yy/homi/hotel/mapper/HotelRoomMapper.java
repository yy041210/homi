package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.HotelRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HotelRoomMapper extends BaseMapper<HotelRoom> {

    @Update("update hotel_room set status = #{newStatus} where id =#{id}")
    int changeStatus(@Param(("id")) String id,@Param("newStatus") Integer newStatus);
}
