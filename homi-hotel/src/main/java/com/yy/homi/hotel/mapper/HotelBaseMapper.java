package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.HotelBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.security.core.parameters.P;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    @Update("update hotel_base set status = #{newStatus} where id = #{id}")
    int changeStatus(@Param("id") String id, @Param("newStatus") int newStatus);

    @Select("select city_id,count(*) as count from hotel_base where status = 0 group by city_id;")
    List<Map<String,Long>> countHotelByCity();
}
