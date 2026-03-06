package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysDistrict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 区县表 Mapper 接口
 */
@Mapper
public interface SysDistrictMapper extends BaseMapper<SysDistrict> {

    /**
     * 根据城市ID查询区县
     */
    List<SysDistrict> selectByCityId(@Param("cityId") Integer cityId);

    /**
     * 根据名称查询区县
     */
    List<SysDistrict> selectByName(@Param("name") String name);

    /**
     * 统计每个区县的酒店数量（需要关联hotel_base表）
     */
    @Select("SELECT d.id, d.name, COUNT(h.id) as hotel_count " +
            "FROM sys_district d " +
            "LEFT JOIN hotel_base h ON d.id = h.district_id " +
            "GROUP BY d.id, d.name")
    List<Map<String, Object>> selectDistrictWithHotelCount();

    /**
     * 批量插入或更新
     */
    int batchInsertOrUpdate(@Param("list") List<SysDistrict> list);
}