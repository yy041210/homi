package com.yy.homi.hotel.domain.dto.request;

import com.yy.homi.hotel.domain.entity.*;
import lombok.Data;
import java.util.List;

@Data
public class HotelInsertDTO {
    // 酒店基本信息 (hotel_base)
    private HotelBase baseInfo;

    // 酒店初始统计数据 (hotel_stats)
    private HotelStats stats;

    // 房型列表 (hotel_room)
    private List<HotelRoom> rooms;

    // 设施列表 (hotel_facility)
    private List<HotelFacility> facilities;

    // 相册列表 (hotel_album)
    private List<HotelAlbum> albums;

    // 周边信息 (hotel_surrounding)
    private List<HotelSurrounding> surroundings;
}