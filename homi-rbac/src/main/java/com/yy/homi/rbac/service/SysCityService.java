package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysCity;

import java.util.List;
import java.util.Map;

/**
 * 城市表 Service 接口
 */
public interface SysCityService extends IService<SysCity> {


    /**
     * 根据名称查询城市
     */
    List<SysCity> findByName(String name);

    /**
     * 获取所有启用的城市
     */
    List<SysCity> findAllEnabled();

    /**
     * 获取城市及其区县数量
     */
    List<Map<String, Object>> getCityWithDistrictCount();

    /**
     * 根据城市ID获取完整的城市区县树
     */
    Map<String, Object> getCityTree(Integer cityId);

    /**
     * 批量导入城市数据
     */
    boolean batchImport(List<SysCity> cityList);

    R getIdByCityNameAndProId(String cityName, Integer provinceId);

    R getCitiesByProId(Integer provinceId);

    R getAllCities();

    R deleteById(Integer cityId);

    R getInfoById(Integer cityId);
}