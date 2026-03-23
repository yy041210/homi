package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelRoomPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelRoom;

import java.util.List;

public interface HotelRoomService extends IService<HotelRoom> {
    R pageList(HotelRoomPageListReqDTO reqDTO);

    R deleteById(String id);

    R deleteByIds(List<String> ids);
}
