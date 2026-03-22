package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 酒店评论实体类
 * 兼容爬虫数据与系统原生评价
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_comment")
public class HotelComment {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String hotelId;      // 关联 HotelBase.id
    private String userId;       // 评论者用户ID
    private String userName;   //用户名

    /** 评分维度 - 爬虫数据只有平均分时，可将平均分同步填充至以下四项 */
    private Float commentScore;             // 综合评分 (对应爬虫 score)
    private Float hygieneScore;      // 卫生评分
    private Float deviceScore;       // 设施评分
    private Float environmentScore;  // 环境评分
    private Float serviceScore;      // 服务评分

    /** 评论内容 */
    private String commentContext;          // 评价内容
    private String roomId;   //关联的房型id
    private String roomName;         // 评价时入住的房型名称

    /** 状态与统计 */
    private String travelType;   //出游类型 商务出差,家庭亲子....

    private Integer likeCount;       // 点赞数量

    private Date checkInTime;  //入住时间

    /** 时间字段 */
    private Date publishTime;        // 评价发布时间

}