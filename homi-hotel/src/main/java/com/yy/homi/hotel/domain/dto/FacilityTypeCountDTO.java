package com.yy.homi.hotel.domain.dto;

import lombok.Data;

@Data
public class FacilityTypeCountDTO {
    // 明确使用 String 接收，对应数据库的 ID
    private String hotelFacilityTypeId;
    // 明确使用 Integer 或 Long 接收统计数
    private Integer count;
}