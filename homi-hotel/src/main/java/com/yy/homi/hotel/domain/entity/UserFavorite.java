package com.yy.homi.hotel.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("user_favorite")
public class UserFavorite implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;        // 主键 ID

    private String userId;    // 用户 ID
    
    private String hotelId;   // 酒店 ID

    private Date createTime;  // 收藏时间
}