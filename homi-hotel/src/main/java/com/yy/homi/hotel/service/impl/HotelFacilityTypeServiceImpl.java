package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.hotel.domain.entity.HotelFacilityType;
import com.yy.homi.hotel.mapper.HotelFacilityTypeMapper;
import com.yy.homi.hotel.service.HotelFacilityTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 酒店设施类型 Service 实现类
 */
@Service
public class HotelFacilityTypeServiceImpl extends ServiceImpl<HotelFacilityTypeMapper, HotelFacilityType> implements HotelFacilityTypeService {

    @Override
    public List<HotelFacilityType> findAllEnabled() {
        // 这里可以根据业务需求添加条件，比如状态为启用的
        // 如果实体类中有status字段可以添加条件，目前没有则查询全部并按seq排序
        LambdaQueryWrapper<HotelFacilityType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(HotelFacilityType::getSeq);
        return baseMapper.selectList(queryWrapper);
    }
}