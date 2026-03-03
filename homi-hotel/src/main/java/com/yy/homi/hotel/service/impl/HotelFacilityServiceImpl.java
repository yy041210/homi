package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.hotel.domain.entity.HotelFacility;
import com.yy.homi.hotel.mapper.HotelFacilityMapper;
import com.yy.homi.hotel.service.HotelFacilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 酒店设施 Service 实现类
 */
@Service
public class HotelFacilityServiceImpl extends ServiceImpl<HotelFacilityMapper, HotelFacility> implements HotelFacilityService {

    @Override
    public List<HotelFacility> getByHotelId(String hotelId) {
        LambdaQueryWrapper<HotelFacility> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HotelFacility::getHotelId, hotelId);
        // 如果需要按状态过滤，可以添加 status = 1 的条件
        // queryWrapper.eq(HotelFacility::getStatus, 1);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<HotelFacility> getByHotelIdAndType(String hotelId, String facilityTypeId) {
        LambdaQueryWrapper<HotelFacility> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HotelFacility::getHotelId, hotelId)
                .eq(HotelFacility::getHotelFacilityTypeId, facilityTypeId);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveHotelFacilities(String hotelId, List<HotelFacility> facilityList) {
        if (CollectionUtils.isEmpty(facilityList)) {
            return false;
        }

        // 先删除该酒店原有的所有设施
        removeByHotelId(hotelId);

        // 为每个设施设置酒店ID
        facilityList.forEach(facility -> facility.setHotelId(hotelId));

        // 批量保存
        return saveBatch(facilityList);
    }

    @Override
    public boolean removeByHotelId(String hotelId) {
        LambdaQueryWrapper<HotelFacility> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HotelFacility::getHotelId, hotelId);
        return remove(queryWrapper);
    }
}