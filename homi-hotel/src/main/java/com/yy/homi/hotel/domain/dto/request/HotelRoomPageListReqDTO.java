package com.yy.homi.hotel.domain.dto.request;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 房型分页查询请求对象
 */
@Data
public class HotelRoomPageListReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNo = 1;

    @NotNull(message = "每页条数不能为空")
    private Integer pageSize = 10;

    private String hotelId;

    private String name;

    private Integer status;

    private String bedType;

    private String window;

    private Integer maxOccupancy;

    private Date beginTime;

    private Date endTime;
}