package com.yy.homi.hotel.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 酒店后台管理展示对象
 * 聚合了基本信息、经营数据、地区名称及多张封面图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelVO {

    // --- 1. 基础信息 (来自 HotelBase) ---
    private String id;           // 酒店主键ID
    private String name;         // 酒店中文名
    private String nameEn;       // 酒店英文名
    private Integer star;        // 酒店星级 (1-5)
    private String phone;        // 酒店电话
    private String address;      // 详细地址
    private Integer status;      // 状态：0-禁用(下架), 1-启用(上架)
    private Integer roomCount;   // 客房总数
    private Integer openYear;    // 开业年份
    private Double lat;          // 纬度
    private Double lng;          // 经度

    // --- 2. 封面图集 (来自 HotelAlbum) ---
    /**
     * 封面图 URL 集合（通常取分类为“精选”的前 5 张）
     */
    private List<String> picUrls;

    // --- 3. 经营统计 (来自 HotelStats) ---
    private Integer minPrice;       // 最低起步价
    private Float hygieneScore;  //卫生得分
    private Float deviceScore;  //设施得分
    private Float environmentScore;  //环境得分
    private Float serviceScore;  //服务得分
    private Float commentScore;     // 综合评分
    private Integer commentCount;   // 评价总数
    private String commentDescription; // 评分描述 (如：“超棒”)
    private String tagTitle;        // 评价摘要 (如：“干净卫生”)

    // --- 4. 地理位置 (冗余文字说明) ---
    private Integer provinceId;
    private Integer cityId;
    private Integer districtId;
    private String provinceName;    // 省份名称
    private String cityName;        // 城市名称
    private String districtName;    // 区县名称
    
    /**
     * 完整区域路径，例如：“河南省 / 濮阳市 / 华龙区”
     */
    private String regionPath;

    // --- 5. 管理元数据 ---
    private String createBy;     // 创建人
    private Date createTime;     // 创建时间
    private Date updateTime;     // 最后更新时间
}