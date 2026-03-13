package com.yy.homi.hotel.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.util.List;

/**
 * 酒店 Elasticsearch 文档模型
 * 整合了 HotelBase, HotelStats, HotelRoom, HotelFacility, HotelSurrounding 的核心数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "hotel")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelDoc {

    @Id
    private String id; // 对应 HotelBase.id

    // --- 基础检索字段 ---
    @Field(type = FieldType.Text, analyzer = "ik_max_word", copyTo = "all")
    private String name; // 酒店名

    @Field(type = FieldType.Text, copyTo = "all")
    private String nameEn; // 英文名

    @Field(type = FieldType.Integer)
    private Integer star; // 星级

    @Field(type = FieldType.Keyword, index = false)
    private String address; // 地址（不参与搜索，仅展示）

    @Field(type = FieldType.Text, analyzer = "ik_smart", copyTo = "all")
    private String description; // 简介

    // --- 地理位置与区域 ---
    @GeoPointField
    private String location; // 经纬度拼接 "lat,lng"

    @Field(type = FieldType.Integer)
    private Integer cityId; // 城市ID，用于快速地区筛选

    // --- 统计与价格 (来自 HotelStats) ---
    @Field(type = FieldType.Float)
    private Float commentScore; // 评分

    @Field(type = FieldType.Integer)
    private Integer minPrice; // 起步价

    @Field(type = FieldType.Keyword)
    private String tagTitle; // 评价摘要（如“干净卫生”）

    // --- 聚合标签 (用于侧边栏筛选) ---
    @Field(type = FieldType.Keyword)
    private List<String> facilities; // 设施名称集合（来自 HotelFacility）

    @Field(type = FieldType.Keyword)
    private List<String> roomTypes; // 房型名称集合（来自 HotelRoom）

    @Field(type = FieldType.Keyword)
    private List<String> surroundings; // 周边交通/景点名称（来自 HotelSurrounding）

    // --- 图片展示5张 (来自 HotelAlbum) ---
    @Field(type = FieldType.Keyword, index = false)
    private List<String> picUrl; // 酒店主图

    // --- 推荐系统与搜索优化 ---
    @Field(type = FieldType.Double)
    private Double popularityScore; // Spark 计算的人气权重分

    /**
     * ES 自动补全专用字段
     */
    @CompletionField(analyzer = "completion_analyzer")
    private Completion suggestion;

    /**
     * 万能搜索字段 (copyTo 目标)
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String all;
}