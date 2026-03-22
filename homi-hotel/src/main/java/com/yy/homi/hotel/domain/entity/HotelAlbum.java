package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

//酒店相册表
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("hotel_album")
public class HotelAlbum {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String hotelId;      //关联酒店id
    private String roomId;     // 关联房型ID：为 null 时表示酒店公共图，不为空时表示房型图
    private String commentId;   //用户上传采用
    private Integer seq;  //排序
    private Integer source;       // 来源：酒店上传/用户上传
    private Integer category;     // 分类：精选/其他/房间
    private String imageId;      //图片id
    private String imageUrl;     //图片url

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

}
