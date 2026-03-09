package com.yy.homi.rbac.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysCity;
import com.yy.homi.rbac.domain.entity.SysDistrict;
import com.yy.homi.rbac.domain.entity.SysProvince;
import com.yy.homi.rbac.feign.HotelBaseFeign;
import com.yy.homi.rbac.mapper.SysCityMapper;
import com.yy.homi.rbac.mapper.SysProvinceMapper;
import com.yy.homi.rbac.service.SysCityService;
import com.yy.homi.rbac.service.SysDistrictService;
import com.yy.homi.rbac.service.SysProvinceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 省份表 Service 实现类
 */
@Slf4j
@Service
public class SysProvinceServiceImpl extends ServiceImpl<SysProvinceMapper, SysProvince> implements SysProvinceService {

    @Autowired
    private  SysCityService cityService;
    @Autowired
    private  SysDistrictService districtService;
    @Autowired
    private  SysProvinceMapper provinceMapper;
    @Autowired
    private SysCityMapper sysCityMapper;
    @Autowired
    private HotelBaseFeign hotelBaseFeign;


    @Override
    public List<SysProvince> findByNameLike(String name) {
        return provinceMapper.selectByNameLike(name);
    }

    @Override
    public List<SysProvince> findAllEnabled() {
        return lambdaQuery()
                .eq(SysProvince::getStatus, 1)
                .orderByAsc(SysProvince::getSort)
                .list();
    }

    @Override
    public List<Map<String, Object>> getProvinceWithCityCount() {
        return provinceMapper.selectProvinceWithCityCount();
    }

    @Override
    public Map<String, Object> getProvinceTree(Integer provinceId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取省份信息
        SysProvince province = this.getById(provinceId);
        if (province == null) {
            return result;
        }
        
        result.put("province", province);
        
        // 获取该省份下的所有城市
        List<SysCity> cities = cityService.lambdaQuery()
                .eq(SysCity::getProvinceId, provinceId)
                .eq(SysCity::getStatus, 1)
                .orderByAsc(SysCity::getSort)
                .list();
        
        // 为每个城市获取其区县
        for (SysCity city : cities) {
            List<SysDistrict> districts = districtService.lambdaQuery()
                    .eq(SysDistrict::getCityId, city.getId())
                    .eq(SysDistrict::getStatus, 1)
                    .orderByAsc(SysDistrict::getSort)
                    .list();
            city.setChildren(districts); // 需要在SysCity中添加 @TableField(exist=false) 的 districts 字段
        }
        
        result.put("cities", cities);
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchImport(List<SysProvince> provinceList) {
        if (provinceList == null || provinceList.isEmpty()) {
            return false;
        }
        
        try {
            return provinceMapper.batchInsertOrUpdate(provinceList) > 0;
        } catch (Exception e) {
            log.error("批量导入省份数据失败", e);
            throw new RuntimeException("批量导入失败", e);
        }
    }

    @Override
    public R getIdByProName(String provinceName) {
        if(StrUtil.isEmpty(provinceName)){
            return R.fail("省名不能为空");
        }
        LambdaQueryWrapper<SysProvince> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysProvince::getName,provinceName);
        SysProvince sysProvince = provinceMapper.selectOne(wrapper);
        if(sysProvince == null){
            return R.fail("未查询到"+provinceName);
        }
        return R.ok(sysProvince.getId());
    }

    @Override
    public R getAllProvinces() {
        List<SysProvince> sysProvinces = provinceMapper.selectList(new LambdaQueryWrapper<SysProvince>().orderByAsc(SysProvince::getSort));
        return R.ok(sysProvinces);
    }

    @Override
    public R deleteById(Integer provinceId) {
        if(provinceId == null){
            return  R.fail("省编码不能为空！");
        }

        List<SysCity> sysCities = sysCityMapper.selectByProvinceId(provinceId);
        if(CollectionUtil.isNotEmpty(sysCities)){
            return R.fail("当前省有关联的市，无法删除！");
        }
        R r = hotelBaseFeign.getByProvinceId(provinceId);
        if(r.getCode() != HttpStatus.OK.value()){
            return R.fail("远程查询当前省关联酒店失败！");
        }
        List<Object> hotelBases = (List<Object>) r.getData();

        if(CollectionUtil.isNotEmpty(hotelBases)){
            return R.fail("当前省有关联的酒店，无法删除！");
        }
        provinceMapper.deleteById(provinceId);
        return R.ok("删除成功！");
    }

    @Override
    public R getInfoById(Integer provinceId) {
        if(provinceId == null){
            return R.fail("省编码不能为空！");
        }
        SysProvince sysProvince = this.getById(provinceId);
        if(sysProvince == null){
            return R.fail("省编码对应的省不存在！");
        }
        return R.ok(sysProvince);
    }
}