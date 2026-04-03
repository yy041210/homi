package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("user_action_log")
public class UserActionLog implements Serializable {

    public static final String VIEW_ACTION = "VIEW_DETAIL";
    public static final String FAVORITE_ACTION = "FAVORITE"; //收藏
    public static final String CLICK_TRIP_ACTION = "CLICK_TRIP"; //跳转携程
    public static final String SEARCH_ACTION = "SEARCH"; //搜索

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;

    // --- 归属的主体：酒店 ---
    private String hotelId;       // hotelId
    private String hotelName;     // 酒店名称
    private Integer star;         // 酒店星级

    // --- 触发行为的房型上下文
    private String roomId;          // 房型id
    private String roomName;        // 房型名称
    private String bedType;         // 床型
    private Double showPrice;       // 搜索页显示的那个房型价格

    // --- 行为属性 ---
    private String actionType;      // VIEW_DETAIL（浏览）, FAVORITE（收藏）, CLICK_TRIP（跳转携程），
    private Double actionWeight;    // 权重分

    // --- 酒店综合特征 ---
    private String cityId;
    private String cityName;
    private Float hygieneScore;  //卫生得分
    private Float deviceScore;  //设施得分
    private Float environmentScore;  //环境得分
    private Float serviceScore;  //服务得分
    private Float commentScore;    // 酒店综合评分
    private Integer commentCount; //评论数
    private Integer maxOccupancy;  //最大可住人数
    private Integer minArea;   //最小面积
    private Integer maxArea;   //最大面积
    private String location;  //经纬度  34.123,111.54

    //搜索操作独有
    private String hotelFacilities;  //设施1，设施2
    private String roomFacilities;   //设施1，设施2

    private Date createTime;


    /**
     * 根据行为类型字符串获取对应的权重分
     */
    public static Double getWeightByType(String actionType) {
        if (actionType == null) {
            return 0.0;
        }

        switch (actionType.toUpperCase()) {
            case VIEW_ACTION:
                return 1.0;
            case FAVORITE_ACTION:
                return 5.0;
            case CLICK_TRIP_ACTION:
                return 8.0;
            default:
                return 0.0;
        }
    }

}