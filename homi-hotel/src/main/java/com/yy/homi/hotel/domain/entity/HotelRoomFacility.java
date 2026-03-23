package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * 房型设施详情实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_room_facility")
public class HotelRoomFacility implements Serializable {

    /**
     * 自增主键 (可选，建议数据库自带)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;


    /**
     * roomId关联的房型id
     */
    private String roomId;

    /**
     * 父级分类 (如：洗浴用品、媒体科技)
     */
    private String facilityType;

    /**
     * 设施名称 (如：牙刷、小冰箱)
     */
    private String name;

    /**
     * 状态：0-启用，1-禁用
     */
    private Integer status;

    /**
     * 其他标签：存储爬虫中的 tags 字段 (如：免费|需预约)
     */
    private String tags;

    /**
     * 排序值：对应爬虫中的 seq
     */
    private Integer seq;
}