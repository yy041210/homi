package com.yy.homi.hotel.domain.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;


@Data
public class HotelFacilityUpdateReqDTO {

    @NotBlank(message = "设施id不能为空！")
    private String id;

    private String hotelId; //关联的酒店id

    private String hotelFacilityTypeId; //关联的设备类型 id

    /**
     * 设施名称：如 "停车场", "智能马桶", "健身房"
     */
    private String name;

    private String icon; //该类型对应的icon (如 "icon-basic")

    private String tags;  //标签 例如： 收费,每天80

    /**
     * 状态：启用/禁用
     */
    private Integer status;

    private String imageUrl;

    private Integer seq;

}
