package com.yy.homi.hotel.domain.entity;

import com.yy.homi.hotel.domain.vo.HotelVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelRank {
    /**
     * 排名
     */
    private Integer rank;

    /**
     * 酒店ID
     */
    private String hotelId;

    private HotelVO hotelVO;

    /**
     * 评论数量
     */
    private Integer commentCount;

    /**
     * 预订数量
     */
    private Integer bookingCount;

    /**
     * 收藏数量
     */
    private Integer favoriteCount;


}