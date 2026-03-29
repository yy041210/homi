package com.yy.homi.hotel.domain.dto.request;

import lombok.Data;
import java.util.List;

/**
 * 酒店搜索es分页请求对象
 */
@Data
public class HotelDocPageListReqDTO {

    // --- 1. 基础分页 ---
    private Integer page = 1;      // 当前页码
    private Integer size = 10;     // 每页条数

    // --- 2. 核心搜索 ---
    private String key;            // 搜索框输入的关键字（匹配 name, address, businessDistrict 等）

    // --- 3. 位置过滤 ---
    private Integer provinceId;    //省id（精确匹配）
    private Integer cityId;        // 城市ID（精确匹配）
    private Integer districtId;    // 区域ID（精确匹配）
    
    // --- 4. 条件筛选 ---
    private Integer star;          // 星级（1-5）
    private Integer minPrice;      // 最低价格
    private Integer maxPrice;      // 最高价格
    private String bedType;        //大床，单人床，特大床，多人床，双床
    private List<String> facilities; // 设施多选（如：["停车场", "WiFi"]）
    private List<String> roomFacilities;  //房型设施筛选

    //5.侧边栏专属筛选参数 ---

    /** 对应：4.7分以上 / 4.5分以上 */
    private Float minScore;

    /** 对应：≥ 25㎡ / ≥ 35㎡ / ≥ 75㎡ */
    private Integer minRoomSize;

    // 6. 排序与定位 ---
    private String sortBy;         // 排序字段：default(综合), commentScore(评价分从高到低),commentCount(评价数从高到低), priceAsc(价格从低到高),priceDesc(价格从高到低),distance(距离最近)

    //7. 半径几公里内
    private Integer rangeRadius;
    
    // 用户当前位置（用于计算距离和按距离排序）
    // 格式: "31.22,121.44"
    private String location; 

    /**
     * 获取 ES 的起始偏移量
     */
    public Integer getFrom() {
        return (page - 1) * size;
    }
}