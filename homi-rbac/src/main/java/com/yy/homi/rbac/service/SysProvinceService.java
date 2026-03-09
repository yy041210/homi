package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysProvince;

import java.util.List;
import java.util.Map;

/**
 * 省份表 Service 接口
 */
public interface SysProvinceService extends IService<SysProvince> {

    /**
     * 根据名称模糊查询
     */
    List<SysProvince> findByNameLike(String name);

    /**
     * 获取所有启用的省份
     */
    List<SysProvince> findAllEnabled();

    /**
     * 获取省份及其城市数量
     */
    List<Map<String, Object>> getProvinceWithCityCount();

    /**
     * 根据省份ID获取完整的省市区树
     */
    Map<String, Object> getProvinceTree(Integer provinceId);

    /**
     * 批量导入省份数据
     */
    boolean batchImport(List<SysProvince> provinceList);

    R getIdByProName(String provinceName);

    R getAllProvinces();

    R deleteById(Integer provinceId);

    R getInfoById(Integer provinceId);
}