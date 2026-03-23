package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelFacilityPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelFacility;

import java.util.List;

/**
 * 酒店设施 Service 接口
 */
public interface HotelFacilityService extends IService<HotelFacility> {

    /**
     * 根据酒店ID查询设施列表
     * @param hotelId 酒店ID
     * @return 设施列表
     */
    List<HotelFacility> getByHotelId(String hotelId);

    /**
     * 根据酒店ID和设施类型ID查询设施
     * @param hotelId 酒店ID
     * @param facilityTypeId 设施类型ID
     * @return 设施列表
     */
    List<HotelFacility> getByHotelIdAndType(String hotelId, String facilityTypeId);

    /**
     * 批量保存酒店设施
     * @param hotelId 酒店ID
     * @param facilityList 设施列表
     * @return 是否成功
     */
    boolean batchSaveHotelFacilities(String hotelId, List<HotelFacility> facilityList);

    /**
     * 删除酒店的所有设施
     * @param hotelId 酒店ID
     * @return 删除数量
     */
    boolean removeByHotelId(String hotelId);

    R pageList(HotelFacilityPageListReqDTO reqDTO);

}