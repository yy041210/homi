package com.yy.homi.hotel.domain.entity;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 酒店 Elasticsearch 文档模型 - 适配卡片展示设计
 * 以房型扁平化处理，最后按hotelId折叠处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "hoteldoc")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelDoc {

    @Id
    private String id;  //

    @Field(type = FieldType.Keyword)
    private String hotelId; // 存 HotelBase.id，用于折叠去重

    // --- 1. 基础检索与展示 ---
    @Field(type = FieldType.Text, analyzer = "ik_max_word", copyTo = "all")
    private String name; // 酒店名，如“桔子酒店（北京平谷万达广场店）”

    @Field(type = FieldType.Text, analyzer = "ik_max_word", copyTo = "all")
    private String nameEn;

    @Field(type = FieldType.Integer)
    private Integer star; // 星级

    @Field(type = FieldType.Text, analyzer = "ik_max_word", copyTo = "all")
    private String address; // 详细地址

    @Field(type = FieldType.Keyword)
    private String businessDistrict; // 商圈，如“平谷万达广场/世纪广场”

    // --- 2. 评分与评价 (卡片左上角蓝色区域) ---
    @Field(type = FieldType.Float)
    private Float commentScore; // 综合评分，如 4.9
    @Field(type = FieldType.Float)
    private Float hygieneScore;  //卫生得分
    @Field(type = FieldType.Float)
    private Float deviceScore;  //设施得分
    @Field(type = FieldType.Float)
    private Float environmentScore;  //环境得分
    @Field(type = FieldType.Float)
    private Float serviceScore;  //服务得分

    @Field(type = FieldType.Keyword)
    private String commentDescription; // 评分描述，如“超棒”

    @Field(type = FieldType.Integer)
    private Integer commentCount; // 评论条数，如“566条点评”

    @Field(type = FieldType.Text)
    private String tagTile; // 评价摘要，如“停车方便，马路对面就是万达广场”

    // --- 3. 地理位置 ---
    @GeoPointField
    private String location; // 经纬度拼接 "lat,lng"

    @Field(type = FieldType.Integer)
    private Integer provinceId; // 省
    @Field(type = FieldType.Keyword)
    private String provinceName;

    @Field(type = FieldType.Integer)
    private Integer cityId; // 市
    @Field(type = FieldType.Keyword)
    private String cityName;

    @Field(type = FieldType.Integer)
    private Integer districtId; // 区
    @Field(type = FieldType.Keyword, copyTo = "all")
    private String districtName;

    //酒店设施 聚合搜索专用 (用于侧边栏筛选)
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"),
            otherFields = {@InnerField(suffix = "raw", type = FieldType.Keyword)}
    )
    private List<String> facilities;
    //图片展示
    @Field(type = FieldType.Keyword, index = false)
    private List<String> picUrls; // 酒店主图轮播集


    //房型数据
    @Field(type = FieldType.Text, analyzer = "ik_max_word", copyTo = "all")
    private String roomName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", copyTo = "all")
    private String bedType;


    @Field(type = FieldType.Integer)
    private Integer maxOccupancy;  //最大可住人数
    @Field(type = FieldType.Integer)
    private Integer minArea;   //最小面积
    @Field(type = FieldType.Integer)
    private Integer maxArea;   //最大面积
    @Field(type = FieldType.Double)
    private Double price; // 价格

    //房型设施 聚合搜索专用 (用于侧边栏筛选)
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"),
            otherFields = {@InnerField(suffix = "raw", type = FieldType.Keyword)}
    )
    private List<String> roomFacilities;


    //推荐与搜索优化 ---
    @Field(type = FieldType.Double)
    private Double popularityScore; // 排序权重

    @CompletionField(analyzer = "completion_analyzer")
    private Completion suggestion; // 搜索自动补全

    @Field(type = FieldType.Text, analyzer = "my_analyzer", searchAnalyzer = "ik_smart")
    private String all; // 万能搜索字段

    private Double distance;  //不是文档字段用于计算直线距离

    /**
     * 将数据库实体转换为 ES 文档模型
     *
     * @return 扁平化的 HotelDoc
     */
    public static HotelDoc build(HotelRoom hotelRoom,
                                 HotelBase hotelBase,
                                 HotelStats hotelStats,
                                 List<String> hotelFacilities,
                                 List<String> hotelRoomFacilities) {
        // 1. 基础字段映射 (从 HotelBase 和 HotelRoom 获取)
        HotelDoc doc = HotelDoc.builder()
                .id(hotelRoom.getId()) // 核心：使用房型ID作为文档唯一标识
                .hotelId(hotelBase.getId())
                .name(hotelBase.getName())
                .nameEn(hotelBase.getNameEn())
                .star(hotelBase.getStar())
                .address(hotelBase.getAddress())
                .businessDistrict(null)   //没有商圈
                // 经纬度处理：ES 要求 "lat,lng" 格式
                .location(hotelBase.getLat() + "," + hotelBase.getLng())
                // 行政区划
                .provinceId(hotelBase.getProvinceId())
//                .provinceName(base.getProvinceName())
                .cityId(hotelBase.getCityId())
//                .cityName(base.getCityName())
                .districtId(hotelBase.getDistrictId())
//                .districtName(base.getDistrictName())
                // 房型具体数据
                .roomName(hotelRoom.getName())
                .bedType(hotelRoom.getBedType())
                .maxOccupancy(hotelRoom.getMaxOccupancy())
                .minArea(hotelRoom.getMinArea())
                .maxArea(hotelRoom.getMaxArea())
                .price(hotelRoom.getPrice()) // 存储分或元，取决于你的业务逻辑
//                .picUrls(base.getPicUrls())    // 酒店主图  后续再查
                .popularityScore(null)  //先不加权重
                .build();

        // 2.从HotelStats 评分与描述
        if (hotelStats != null) {
            doc.setCommentScore(hotelStats.getCommentScore());
            doc.setHygieneScore(hotelStats.getHygieneScore());
            doc.setServiceScore(hotelStats.getServiceScore());
            doc.setEnvironmentScore(hotelStats.getEnvironmentScore());
            doc.setDeviceScore(hotelStats.getDeviceScore());
            doc.setCommentCount(hotelStats.getCommentCount());
            doc.setCommentDescription(hotelStats.getCommentDescription());
            doc.setTagTile(hotelStats.getTagTitle());
        }

        // 3. 酒店设施 和 房型设施
        if (hotelFacilities != null) {
            doc.setFacilities(hotelFacilities);
        } else {
            doc.setFacilities(new ArrayList<>());
        }
        if (hotelRoomFacilities != null) {
            doc.setRoomFacilities(hotelRoomFacilities);
        } else {
            doc.setRoomFacilities(new ArrayList<>());
        }

        // 4. 处理搜索建议 (Completion Suggester)
        String rawName = hotelBase.getName(); // 例如：桔子酒店（北京平谷万达广场店）

        //构造建议词集合 (使用 Set 去重)
        Set<String> suggestSet = new HashSet<>();
        suggestSet.add(rawName); // 全称肯定要加

        //处理括号逻辑：拆分出“酒店主名”和“分店名”
        if (rawName.contains("（") || rawName.contains("(")) {
            // 替换中文括号为英文，方便统一分割
            String normalizedName = rawName.replace("（", "(").replace("）", ")");
            String mainName = StrUtil.subBefore(normalizedName, "(", false); // 桔子酒店
            String branchName = StrUtil.subBetween(normalizedName, "(", ")"); // 北京平谷万达广场店

            if (StrUtil.isNotBlank(mainName)) suggestSet.add(mainName);
            if (StrUtil.isNotBlank(branchName)) suggestSet.add(branchName);
        }
        if (StrUtil.isNotBlank(hotelBase.getAddress())) {
            suggestSet.add(hotelBase.getAddress());
        }
        doc.setSuggestion(new Completion(new ArrayList<>(suggestSet)));

        // 5. 处理万能搜索字段 'all'
        // 将需要被全文索引的字段拼接，配合 @Field(copyTo = "all") 使用或手动拼接
        // 这里如果是手动维护 all 字段：
        String all = StrUtil.format("{} {} {} {} {}",
                hotelBase.getName(),
//                base.getCityName(),
//                base.getBusinessDistrict(),
                hotelBase.getAddress(),
                hotelBase.getName());
        doc.setAll(all);

        return doc;

    }

}