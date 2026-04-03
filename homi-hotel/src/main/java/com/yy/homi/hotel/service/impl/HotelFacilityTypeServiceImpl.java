package com.yy.homi.hotel.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.FacilityTypeCountDTO;
import com.yy.homi.hotel.domain.entity.HotelFacility;
import com.yy.homi.hotel.domain.entity.HotelFacilityType;
import com.yy.homi.hotel.mapper.HotelFacilityMapper;
import com.yy.homi.hotel.mapper.HotelFacilityTypeMapper;
import com.yy.homi.hotel.service.HotelFacilityTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 酒店设施类型 Service 实现类
 */
@Service
public class HotelFacilityTypeServiceImpl extends ServiceImpl<HotelFacilityTypeMapper, HotelFacilityType> implements HotelFacilityTypeService {

    @Autowired
    private HotelFacilityMapper hotelFacilityMapper;
    @Autowired
    private HotelFacilityTypeMapper hotelFacilityTypeMapper;

    @Override
    public List<HotelFacilityType> findAllEnabled() {
        // 这里可以根据业务需求添加条件，比如状态为启用的
        // 如果实体类中有status字段可以添加条件，目前没有则查询全部并按seq排序
        LambdaQueryWrapper<HotelFacilityType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(HotelFacilityType::getSeq);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public R listAll() {
        List<HotelFacilityType> hotelFacilityTypeList = hotelFacilityTypeMapper.selectList(new LambdaQueryWrapper<HotelFacilityType>().orderByAsc(HotelFacilityType::getSeq));
        int facilityTypeCount = hotelFacilityTypeList.size();

        List<FacilityTypeCountDTO> facilityTypeCountDTOS = hotelFacilityMapper.countGroupByTypeId();

        Map<String, Integer> facilityCountGroupByTypeIdMap = facilityTypeCountDTOS.stream().collect(Collectors.toMap(
                FacilityTypeCountDTO::getHotelFacilityTypeId,
                FacilityTypeCountDTO::getCount
        ));

        int enabledFacility = hotelFacilityMapper.countEnabled();
        int disabledFacility = hotelFacilityMapper.countDisabled();

        JSONObject result = new JSONObject();
        JSONArray facilityTypes = new JSONArray();
        for (HotelFacilityType hotelFacilityType : hotelFacilityTypeList) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(hotelFacilityType);
            if (facilityCountGroupByTypeIdMap.get(hotelFacilityType.getId()) != null) {
                jsonObject.put("facilityCount", facilityCountGroupByTypeIdMap.get(hotelFacilityType.getId()));
            } else {
                jsonObject.put("facilityCount", 0);
            }
            facilityTypes.add(jsonObject);
        }

        result.put("facilityTypes", facilityTypes);
        result.put("facilityTypeCount", facilityTypeCount);
        result.put("facilityCount", enabledFacility + disabledFacility);
        result.put("enabledFacilityCount", enabledFacility);
        result.put("disabledFacilityCount", disabledFacility);

        return R.ok(result);
    }


    @Override
    @Transactional
    public R deleteById(String id) {
        if(StrUtil.isBlank(id)){
            return R.fail("设备类型id不能为空！");
        }

        //删除该类型的所有设备
        hotelFacilityMapper.delete(new LambdaQueryWrapper<HotelFacility>().eq(HotelFacility::getHotelFacilityTypeId,id));

        //删除该类型
        hotelFacilityTypeMapper.deleteById(id);

        return R.ok("删除成功！");
    }

}