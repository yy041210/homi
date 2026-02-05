package com.yy.homi.rbac.domain.dto.request;

import lombok.Data;

import java.util.Date;

@Data
public class ConditionGetMenuTreeReqDTO {

    private String menuName;
    private String menuType;
    private Integer visible;
    private Integer status;
    private Date beginTime;
    private Date endTime;
}
