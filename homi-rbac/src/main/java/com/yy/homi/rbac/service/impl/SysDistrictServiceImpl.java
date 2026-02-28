package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysDistrict;
import com.yy.homi.rbac.mapper.SysDistrictMapper;
import com.yy.homi.rbac.service.SysDistrictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 区县表 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDistrictServiceImpl extends ServiceImpl<SysDistrictMapper, SysDistrict> implements SysDistrictService {

    private final SysDistrictMapper districtMapper;

    @Override
    public List<SysDistrict> findByCityId(Integer cityId) {
        return districtMapper.selectByCityId(cityId);
    }

    @Override
    public List<SysDistrict> findByProvinceId(Integer provinceId) {
        return districtMapper.selectByProvinceId(provinceId);
    }

    @Override
    public List<SysDistrict> findByName(String name) {
        return districtMapper.selectByName(name);
    }

    @Override
    public List<SysDistrict> findAllEnabled() {
        return lambdaQuery()
                .eq(SysDistrict::getStatus, 1)
                .orderByAsc(SysDistrict::getSort)
                .list();
    }

    @Override
    public List<Map<String, Object>> getDistrictWithHotelCount() {
        return districtMapper.selectDistrictWithHotelCount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchImport(List<SysDistrict> districtList) {
        if (districtList == null || districtList.isEmpty()) {
            return false;
        }

        try {
            return districtMapper.batchInsertOrUpdate(districtList) > 0;
        } catch (Exception e) {
            log.error("批量导入区县数据失败", e);
            throw new RuntimeException("批量导入失败", e);
        }
    }

    @Override
    public R getIdByDisNameAndCityId(String districtName, Integer cityId) {
        if (StrUtil.isEmpty(districtName) || cityId == null){
            return R.fail("区域名或城市id不能为空");
        }

        SysDistrict sysDistrict = districtMapper.selectOne(new LambdaQueryWrapper<SysDistrict>().eq(SysDistrict::getName, districtName).eq(SysDistrict::getCityId, cityId));
        if(sysDistrict == null){
            return R.fail("未查询到区域id");
        }
        return R.ok(sysDistrict.getId());
    }
}