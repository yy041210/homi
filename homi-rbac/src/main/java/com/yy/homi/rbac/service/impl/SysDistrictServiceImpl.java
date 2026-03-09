package com.yy.homi.rbac.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysDistrict;
import com.yy.homi.rbac.feign.HotelBaseFeign;
import com.yy.homi.rbac.mapper.SysDistrictMapper;
import com.yy.homi.rbac.service.SysDistrictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 区县表 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDistrictServiceImpl extends ServiceImpl<SysDistrictMapper, SysDistrict> implements SysDistrictService {

    @Autowired
    private  SysDistrictMapper districtMapper;
    @Autowired
    private HotelBaseFeign hotelBaseFeign;


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

    @Override
    public R getDistrictsByCityId(Integer cityId) {
        if(cityId == null ){
            return R.fail("cityId 不能为空！");
        }
        List<SysDistrict> sysDistricts = districtMapper.selectByCityId(cityId);
        return R.ok(sysDistricts);
    }

    @Override
    public R deleteById(Integer districtId) {
        if(districtId == null){
            return  R.fail("区域id不能为空！");
        }
        R r = hotelBaseFeign.getByDistrictId(districtId);
        if(r.getCode() != HttpStatus.OK.value()){
            return R.fail("远程查询关联改县区酒店失败！");
        }
        List<Object> hotelBases = (List<Object>) r.getData();
        if(CollectionUtil.isNotEmpty(hotelBases)){
            return R.fail("当前区县有关联的酒店无法删除！");
        }
        districtMapper.deleteById(districtId);
        return R.ok("删除成功！");
    }

    @Override
    public R getInfoById(Integer districtId) {
        if(districtId == null){
            return R.fail("县区编码不能为空!");
        }
        SysDistrict sysDistrict = this.getById(districtId);
        if(sysDistrict == null){
            return R.fail("县区编码对应的县不存在！");
        }
        return R.ok(sysDistrict);
    }

}