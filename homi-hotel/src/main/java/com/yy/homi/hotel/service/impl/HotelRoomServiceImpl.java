package com.yy.homi.hotel.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelRoomPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelRoom;
import com.yy.homi.hotel.mapper.HotelRoomMapper;
import com.yy.homi.hotel.service.HotelRoomService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelRoomServiceImpl extends ServiceImpl<HotelRoomMapper, HotelRoom> implements HotelRoomService {
    @Override
    public R pageList(HotelRoomPageListReqDTO reqDTO) {
        // 1. 开启分页 (注意：PageHelper 只对紧随其后的第一个 Select 语句有效)
        PageHelper.startPage(reqDTO.getPageNo(), reqDTO.getPageSize());

        // 2. 构造查询条件
        LambdaQueryWrapper<HotelRoom> lqw = new LambdaQueryWrapper<>();

        // 酒店、状态、人数等精确匹配
        lqw.eq(StrUtil.isNotBlank(reqDTO.getHotelId()), HotelRoom::getHotelId, reqDTO.getHotelId());
        lqw.eq(reqDTO.getStatus() != null, HotelRoom::getStatus, reqDTO.getStatus());
        lqw.eq(reqDTO.getMaxOccupancy() != null, HotelRoom::getMaxOccupancy, reqDTO.getMaxOccupancy());

        // 房型名称模糊查询
        lqw.like(StrUtil.isNotBlank(reqDTO.getName()), HotelRoom::getName, reqDTO.getName());

        // 床型模糊匹配（处理乱序字符串）
        if (StrUtil.isNotBlank(reqDTO.getBedType())) {
            String bedType = reqDTO.getBedType().trim();
            String[] keywords = bedType.split("\\s+");
            lqw.and(w -> {
                for (String key : keywords) {
                    w.like(HotelRoom::getBedType, key);
                }
            });
        }

        // 时间范围过滤
        lqw.ge(reqDTO.getBeginTime() != null, HotelRoom::getCreateTime, reqDTO.getBeginTime());
        lqw.le(reqDTO.getEndTime() != null, HotelRoom::getCreateTime, reqDTO.getEndTime());

        // 排序
        lqw.orderByDesc(HotelRoom::getCreateTime);

        // 3. 执行查询 (此时 BaseMapper.selectList 会被分页拦截器拦截)
        List<HotelRoom> list = this.list(lqw);

        // 4. 封装 PageInfo 并返回给 R
        PageInfo<HotelRoom> pageInfo = new PageInfo<>(list);

        return R.ok(pageInfo);
    }
}
