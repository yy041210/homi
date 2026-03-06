package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.RegionInsertReqDTO;
import com.yy.homi.rbac.domain.entity.SysCity;
import com.yy.homi.rbac.domain.entity.SysDistrict;
import com.yy.homi.rbac.domain.entity.SysProvince;
import com.yy.homi.rbac.mapper.SysCityMapper;
import com.yy.homi.rbac.mapper.SysDistrictMapper;
import com.yy.homi.rbac.mapper.SysProvinceMapper;
import com.yy.homi.rbac.service.SysRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysRegionServiceImpl implements SysRegionService {

    @Autowired
    private SysProvinceMapper provinceMapper;
    @Autowired
    private SysCityMapper cityMapper;
    @Autowired
    private SysDistrictMapper districtMapper;

    /**
     * 查询地区树形结构
     */
    public R getRegionTree() {
        // 1. 获取所有省、市、区（假设只查询启用状态的）
        List<SysProvince> allProvinces = provinceMapper.selectList(
                new LambdaQueryWrapper<SysProvince>().eq(SysProvince::getStatus, 1).orderByAsc(SysProvince::getSort)
        );
        List<SysCity> allCities = cityMapper.selectList(
                new LambdaQueryWrapper<SysCity>().eq(SysCity::getStatus, 1).orderByAsc(SysCity::getSort)
        );
        List<SysDistrict> allDistricts = districtMapper.selectList(
                new LambdaQueryWrapper<SysDistrict>().eq(SysDistrict::getStatus, 1).orderByAsc(SysDistrict::getSort)
        );

        // 2. 将区县按城市ID进行分组
        Map<Integer, List<SysDistrict>> districtMap = allDistricts.stream()
                .collect(Collectors.groupingBy(SysDistrict::getCityId));

        // 3. 将城市归类到省份，并注入区县
        Map<Integer, List<SysCity>> cityMap = allCities.stream()
                .peek(city -> city.setChildren(districtMap.get(city.getId()))) // 注入区县列表
                .collect(Collectors.groupingBy(SysCity::getProvinceId));

        // 4. 将城市列表注入省份
        allProvinces.forEach(province -> {
            // 注意：需在 SysProvince 类中添加 children 字段
            province.setChildren(cityMap.get(province.getId()));
        });

        return R.ok(allProvinces);
    }

    public R searchRegionTree(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return R.ok(Collections.emptyList());
        }

        // 1. 获取全量数据（已按 sort 升序）
        List<SysProvince> allProvinces = provinceMapper.selectList(new LambdaQueryWrapper<SysProvince>().eq(SysProvince::getStatus, 1).orderByAsc(SysProvince::getSort));
        List<SysCity> allCities = cityMapper.selectList(new LambdaQueryWrapper<SysCity>().eq(SysCity::getStatus, 1).orderByAsc(SysCity::getSort));
        List<SysDistrict> allDistricts = districtMapper.selectList(new LambdaQueryWrapper<SysDistrict>().eq(SysDistrict::getStatus, 1).orderByAsc(SysDistrict::getSort));

        // 2. 筛选匹配的 ID 集合
        Set<Integer> matchedProvinceIds = allProvinces.stream()
                .filter(p -> p.getName().contains(keyword) || String.valueOf(p.getId()).equals(keyword))
                .map(SysProvince::getId).collect(Collectors.toSet());

        Set<Integer> matchedCityIds = allCities.stream()
                .filter(c -> c.getName().contains(keyword) || String.valueOf(c.getId()).equals(keyword))
                .map(SysCity::getId).collect(Collectors.toSet());

        Set<Integer> matchedDistrictIds = allDistricts.stream()
                .filter(d -> d.getName().contains(keyword) || String.valueOf(d.getId()).equals(keyword))
                .map(SysDistrict::getId).collect(Collectors.toSet());

        // 3. 构建树形结构并过滤
        // A. 将区县分组（由于 allDistricts 有序，分组后的 List 默认也是有序的）
        Map<Integer, List<SysDistrict>> districtMap = allDistricts.stream()
                .collect(Collectors.groupingBy(SysDistrict::getCityId));

        // B. 构建城市层级
        List<SysCity> filteredCities = allCities.stream()
                .peek(city -> {
                    List<SysDistrict> ds = districtMap.getOrDefault(city.getId(), new ArrayList<>());
                    // 筛选匹配的区县
                    List<SysDistrict> matchedDs = ds.stream()
                            .filter(d -> matchedDistrictIds.contains(d.getId()))
                            .collect(Collectors.toList());
                    city.setChildren(matchedDs); // 注入区县列表
                })
                // 城市本身匹配 或 子区县匹配 则保留
                .filter(city -> matchedCityIds.contains(city.getId()) || (city.getChildren() != null && !city.getChildren().isEmpty()))
                .collect(Collectors.toList());

        // C. 将过滤后的城市按省份分组
        Map<Integer, List<SysCity>> cityMap = filteredCities.stream()
                .collect(Collectors.groupingBy(SysCity::getProvinceId));

        // D. 构建最终省份树并过滤
        List<SysProvince> resultTree = allProvinces.stream()
                .peek(province -> {
                    List<SysCity> cities = cityMap.get(province.getId());
                    province.setChildren(cities); // 注入城市列表
                })
                // 省份本身匹配 或 子城市匹配 则保留
                .filter(province -> matchedProvinceIds.contains(province.getId()) || (province.getChildren() != null && !province.getChildren().isEmpty()))
                .collect(Collectors.toList());

        return R.ok(resultTree);
    }

    @Override
    @Transactional
    public R insertRegion(RegionInsertReqDTO reqDTO) {
        Integer type = reqDTO.getType();
        Integer id = reqDTO.getId();
        String name = reqDTO.getName();
        String nameEn = reqDTO.getNameEn();
        Integer parentId = reqDTO.getParentId();
        Integer status = reqDTO.getStatus();
        Integer sort = reqDTO.getSort();
        BigDecimal centerLng = reqDTO.getCenterLng();
        BigDecimal centerLat = reqDTO.getCenterLat();

        if (type == null || id == null || StrUtil.isEmpty(name)) {
            return R.fail("参数校验失败,类型|编号|名字 字段不能为空！");
        }

        if (type == 1) {
            //新增能省份，不需要parentId
            SysProvince sysProvince = new SysProvince();
            sysProvince.setId(id);
            sysProvince.setName(name);
            sysProvince.setNameEn(nameEn);
            sysProvince.setStatus(status);
            sysProvince.setSort(sort);
            sysProvince.setCenterLng(centerLng);
            sysProvince.setCenterLat(centerLat);
            provinceMapper.insert(sysProvince);
        } else if (type == 2) {
            //市
            if(parentId == null){
                return R.fail("需要选择所属省！");
            }
            SysProvince sysProvince = provinceMapper.selectById(parentId);
            if(sysProvince == null){
                return R.fail("所属省不存在！");
            }
            SysCity sysCity = new SysCity();
            sysCity.setId(id);
            sysCity.setName(name);
            sysCity.setNameEn(nameEn);
            sysCity.setStatus(status);
            sysCity.setProvinceId(parentId);
            sysCity.setCenterLng(centerLng);
            sysCity.setCenterLat(centerLat);
            cityMapper.insert(sysCity);
        } else if (type == 3) {
            //区
            if(parentId == null){
                return R.fail("需要选择所属市！");
            }
            SysCity sysCity = cityMapper.selectById(parentId);
            if(sysCity == null){
                return R.fail("所属市不存在！");
            }
            SysDistrict sysDistrict = new SysDistrict();
            sysDistrict.setId(id);
            sysDistrict.setName(name);
            sysDistrict.setNameEn(nameEn);
            sysDistrict.setStatus(status);
            sysDistrict.setCityId(parentId);
            sysDistrict.setCenterLng(centerLng);
            sysDistrict.setCenterLat(centerLat);
            districtMapper.insert(sysDistrict);
        } else {
            return R.fail("位置新增类型！");
        }
        return R.ok("新增成功！");
    }
}