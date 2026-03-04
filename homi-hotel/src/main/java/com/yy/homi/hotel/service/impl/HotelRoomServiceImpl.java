package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.hotel.domain.entity.HotelRoom;
import com.yy.homi.hotel.mapper.HotelRoomMapper;
import com.yy.homi.hotel.service.HotelRoomService;
import org.springframework.stereotype.Service;

@Service
public class HotelRoomServiceImpl extends ServiceImpl<HotelRoomMapper, HotelRoom> implements HotelRoomService {
}
