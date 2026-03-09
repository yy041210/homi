package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysDistrict;

import java.util.List;
import java.util.Map;

/**
 * 区县表 Service 接口
 */
public interface SysDistrictService extends IService<SysDistrict> {


    /**
     * 根据名称查询区县
     */
    List<SysDistrict> findByName(String name);

    /**
     * 获取所有启用的区县
     */
    List<SysDistrict> findAllEnabled();

    /**
     * 获取区县及其酒店数量
     */
    List<Map<String, Object>> getDistrictWithHotelCount();

    /**
     * 批量导入区县数据
     */
    boolean batchImport(List<SysDistrict> districtList);

    R getIdByDisNameAndCityId(String districtName, Integer cityId);

    R getDistrictsByCityId(Integer cityId);

    R deleteById(Integer districtId);

    R getInfoById(Integer districtId);
}