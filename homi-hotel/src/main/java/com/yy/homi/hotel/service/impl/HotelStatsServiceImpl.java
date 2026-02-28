package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.hotel.domain.entity.HotelStats;
import com.yy.homi.hotel.mapper.HotelStatsMapper;
import com.yy.homi.hotel.service.HotelStatsService;
import org.springframework.stereotype.Service;

@Service
public class HotelStatsServiceImpl extends ServiceImpl<HotelStatsMapper, HotelStats> implements HotelStatsService {
}
