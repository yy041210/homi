package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysCity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 城市表 Mapper 接口
 */
@Mapper
public interface SysCityMapper extends BaseMapper<SysCity> {

    /**
     * 根据省份ID查询城市
     */
    List<SysCity> selectByProvinceId(@Param("provinceId") Integer provinceId);

    /**
     * 根据名称查询城市
     */
    List<SysCity> selectByName(@Param("name") String name);

    /**
     * 统计每个城市的区县数量
     */
    @Select("SELECT c.id, c.name, c.province_id, COUNT(d.id) as district_count " +
            "FROM sys_city c " +
            "LEFT JOIN sys_district d ON c.id = d.city_id " +
            "GROUP BY c.id, c.name, c.province_id")
    List<Map<String, Object>> selectCityWithDistrictCount();

    /**
     * 批量插入或更新
     */
    int batchInsertOrUpdate(@Param("list") List<SysCity> list);
}