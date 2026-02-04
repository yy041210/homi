package com.yy.homi.rbac.domain.vo;

import lombok.Data;

@Data
public class MenuOptionVO {
    private String id;
    private String menuName;
    private String menuType;
    private Integer visible;
    private Integer status;

}
