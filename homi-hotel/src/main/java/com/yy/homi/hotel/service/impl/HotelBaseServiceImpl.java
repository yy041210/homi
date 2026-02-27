package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.service.HotelBaseService;
import org.springframework.stereotype.Service;

@Service
public class HotelBaseServiceImpl extends ServiceImpl<HotelBaseMapper, HotelBase> implements HotelBaseService {
}
