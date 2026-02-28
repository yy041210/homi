package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_surrounding")
public class HotelSurrounding {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String hotelId;

    /**
     * 1-交通 2-景点 3-购物等 (由业务逻辑定义)
     */
    private Integer category;

    private String surroundingName;

    private Double distance;  //单位 m

    private String distanceDesc;

    /**
     * 出行方式 (DRIVE, LINEAR_DISTANCE 等)
     */
    private String arrivalType;

    /**
     *"火车站", "机场", "地铁站" 等
     */
    private String tagName;

    private Double lat;  //纬度
    private Double lon;  //经度

    /**
     * 排序字段：数值越小越靠前 (默认可设为 0 或 999)
     */
    private Integer seq;

    // 定义常量便于逻辑判断
    public static final String TAG_SUBWAY = "地铁站";
    public static final String TAG_RAILWAY = "火车站";
    public static final String TAG_AIRPORT = "机场";
}