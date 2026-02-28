package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysProvince;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 省份表 Mapper 接口
 */
@Mapper
public interface SysProvinceMapper extends BaseMapper<SysProvince> {

    /**
     * 根据名称模糊查询省份
     */
    List<SysProvince> selectByNameLike(@Param("name") String name);

    /**
     * 统计每个省份的城市数量
     */
    @Select("SELECT p.id, p.name, COUNT(c.id) as city_count " +
            "FROM sys_province p " +
            "LEFT JOIN sys_city c ON p.id = c.province_id " +
            "GROUP BY p.id, p.name")
    List<Map<String, Object>> selectProvinceWithCityCount();

    /**
     * 批量插入或更新
     */
    int batchInsertOrUpdate(@Param("list") List<SysProvince> list);
}