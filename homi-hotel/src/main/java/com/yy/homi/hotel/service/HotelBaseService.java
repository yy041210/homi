package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelBasePageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelDocPageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelInsertDTO;
import com.yy.homi.hotel.domain.entity.HotelBase;
import org.springframework.web.multipart.MultipartFile;


public interface HotelBaseService extends IService<HotelBase> {

    R importHotelBaseFromJsonCsv(MultipartFile file);

    R selectHotelPage(HotelBasePageListReqDTO reqDTO);

    R getByDistrictId(Integer districtId);


    R getByCityId(Integer cityId);

    R getByProvinceId(Integer provinceId);

    R saveHotel(HotelInsertDTO hotelInsertDTO);

    R getInfoById(String id);

    R changeStatus(String id);

    R searchPageList(HotelDocPageListReqDTO reqDTO);

    R suggestion(String key);

    R getHotelFacilityFilters();

    R syncHotelDocFromDB();

    R getRecommendHotelList(String userId);
}
