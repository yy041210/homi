package com.yy.homi.hotel.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelFacilityPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelFacility;
import com.yy.homi.hotel.mapper.HotelFacilityMapper;
import com.yy.homi.hotel.service.HotelFacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 酒店设施 Service 实现类
 */
@Service
public class HotelFacilityServiceImpl extends ServiceImpl<HotelFacilityMapper, HotelFacility> implements HotelFacilityService {

    @Autowired
    private HotelFacilityMapper hotelFacilityMapper;


    @Override
    public List<HotelFacility> getByHotelId(String hotelId) {
        LambdaQueryWrapper<HotelFacility> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(HotelFacility::getHotelId, hotelId);
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

    @Override
    public R pageList(HotelFacilityPageListReqDTO reqDTO) {
        // 1. 开启 PageHelper 分页
        PageHelper.startPage(reqDTO.getPageNum(), reqDTO.getPageSize());

        // 2. 构造查询条件
        LambdaQueryWrapper<HotelFacility> lqw = new LambdaQueryWrapper<>();

        // 精确匹配：酒店ID、类型ID、状态
        lqw.eq(StrUtil.isNotBlank(reqDTO.getHotelId()), HotelFacility::getHotelId, reqDTO.getHotelId());
        lqw.eq(StrUtil.isNotBlank(reqDTO.getHotelFacilityTypeId()), HotelFacility::getHotelFacilityTypeId, reqDTO.getHotelFacilityTypeId());
        lqw.eq(reqDTO.getStatus() != null, HotelFacility::getStatus, reqDTO.getStatus());

        // 模糊匹配：设施名称、标签
        lqw.like(StrUtil.isNotBlank(reqDTO.getName()), HotelFacility::getName, reqDTO.getName());
        lqw.like(StrUtil.isNotBlank(reqDTO.getTags()), HotelFacility::getTags, reqDTO.getTags());

        // 3. 排序逻辑处理 (Integer 类型 sortRule)
        if (reqDTO.getSortRule() != null) {
            if (reqDTO.getSortRule() == 1) {
                // 1: 按序号 seq 升序
                lqw.orderByAsc(HotelFacility::getSeq);
            } else if (reqDTO.getSortRule() == 2) {
                // 2: 按创建时间倒序
                lqw.orderByDesc(HotelFacility::getCreateTime);
            }
        } else {
            // 默认排序：先按 seq 升序，再按创建时间倒序
            lqw.orderByAsc(HotelFacility::getSeq).orderByDesc(HotelFacility::getCreateTime);
        }

        // 4. 执行查询
        List<HotelFacility> list = this.list(lqw);

        // 5. 包装分页结果并返回
        PageInfo<HotelFacility> pageInfo = new PageInfo<>(list);
        return R.ok(pageInfo);
    }

    @Override
    public R changeStatus(String id) {
        if (StrUtil.isBlank(id)) {
            return R.fail("设备id不能为空！");
        }

        HotelFacility hotelFacility = this.getById(id);
        if(hotelFacility == null){
            return R.fail("id对应的设备不存在！");
        }
        Integer newStatus = 0;
        Integer status = hotelFacility.getStatus();
        if(status == CommonConstants.STATUS_ENABLED){
            //禁用操作
            newStatus = CommonConstants.STATUS_DISABLED;
            hotelFacilityMapper.changeStatus(id,newStatus);
            return R.ok("禁用成功！");
        }else{
            //启用操作
            hotelFacilityMapper.changeStatus(id,newStatus);
            return R.ok("启用成功！");
        }
    }
}