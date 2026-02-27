package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;



@Data
@TableName("hotel_stats")
@NoArgsConstructor
@AllArgsConstructor
public class HotelStats {

    @TableId(type = IdType.INPUT)
    private String hotelId;  //关联的酒店id
    private Float hygieneScore;  //卫生得分
    private Float deviceScore;  //设施得分
    private Float environmentScore;  //环境得分
    private Float serviceScore;  //服务得分
    private Float commentScore;  // 评分
    private String commentDescription; //评分描述 ”超棒“
    private Integer commentCount; //评价总数
    private String tagTitle;  //评价摘要 “干净卫生，服务热情”
    private Integer minPrice; //最低起步价

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
