package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysCity;
import com.yy.homi.rbac.domain.entity.SysDistrict;
import com.yy.homi.rbac.mapper.SysCityMapper;
import com.yy.homi.rbac.service.SysCityService;
import com.yy.homi.rbac.service.SysDistrictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 城市表 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysCityServiceImpl extends ServiceImpl<SysCityMapper, SysCity> implements SysCityService {

    private final SysDistrictService districtService;
    private final SysCityMapper cityMapper;

    @Override
    public List<SysCity> findByName(String name) {
        return cityMapper.selectByName(name);
    }

    @Override
    public List<SysCity> findAllEnabled() {
        return lambdaQuery()
                .eq(SysCity::getStatus, 1)
                .orderByAsc(SysCity::getSort)
                .list();
    }

    @Override
    public List<Map<String, Object>> getCityWithDistrictCount() {
        return cityMapper.selectCityWithDistrictCount();
    }

    @Override
    public Map<String, Object> getCityTree(Integer cityId) {
        Map<String, Object> result = new HashMap<>();

        // 获取城市信息
        SysCity city = this.getById(cityId);
        if (city == null) {
            return result;
        }

        result.put("city", city);

        // 获取该城市下的所有区县
        List<SysDistrict> districts = districtService.lambdaQuery()
                .eq(SysDistrict::getCityId, cityId)
                .eq(SysDistrict::getStatus, 1)
                .orderByAsc(SysDistrict::getSort)
                .list();

        result.put("districts", districts);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchImport(List<SysCity> cityList) {
        if (cityList == null || cityList.isEmpty()) {
            return false;
        }

        try {
            return cityMapper.batchInsertOrUpdate(cityList) > 0;
        } catch (Exception e) {
            log.error("批量导入城市数据失败", e);
            throw new RuntimeException("批量导入失败", e);
        }
    }

    @Override
    public R getIdByCityNameAndProId(String cityName, Integer provinceId) {
        if (StrUtil.isEmpty(cityName) || provinceId == null) {
            return R.fail("城市名或省份id不能为空");
        }
        SysCity sysCity = cityMapper.selectOne(new LambdaQueryWrapper<SysCity>().eq(SysCity::getName, cityName).eq(SysCity::getProvinceId, provinceId));
        if (sysCity == null) {
            return R.fail("未查询到城市id");
        }
        return R.ok(sysCity.getId());
    }

    @Override
    public R getCitiesByProId(Integer provinceId) {

        if(provinceId == null){
            return R.fail("provinceId 不能为空！");
        }
        List<SysCity> sysCities = cityMapper.selectByProvinceId(provinceId);
        return R.ok(sysCities);
    }
}