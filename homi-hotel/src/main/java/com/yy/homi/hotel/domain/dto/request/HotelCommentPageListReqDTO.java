package com.yy.homi.hotel.domain.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
public class HotelCommentPageListReqDTO implements Serializable {

    /** 酒店ID (必传) */
    private String hotelId;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;


    private String roomId;

    /** * 排序类型 
     * 0: 最新发布 (默认)
     * 1: 评分从高到低
     * 2: 点赞数从高到低
     */
    private Integer sortType = 0;

    /** 出游类型过滤 (如：商务出差、家庭亲子) */
    private String travelType;

    private Date beginTime;
    private Date endTime;

}