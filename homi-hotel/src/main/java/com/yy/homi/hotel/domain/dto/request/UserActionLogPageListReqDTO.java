package com.yy.homi.hotel.domain.dto.request;

import lombok.Data;

@Data
public class UserActionLogPageListReqDTO {
    private Integer pageNum = 1;  // 当前页码
    private Integer pageSize = 10; // 每页条数
    
    // 查询条件字段
    private String userId;
    private String hotelName;
    private String actionType; // VIEW_DETAIL, FAVORITE, CLICK_TRIP
}