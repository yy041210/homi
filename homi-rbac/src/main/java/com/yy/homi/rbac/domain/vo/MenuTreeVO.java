package com.yy.homi.rbac.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTreeVO {
    private String id;
    private String parentId;
    private String menuName;
    private String menuType;
    private String icon;
    private Integer orderNum;
    private String path;
    private String component;
    private Integer visible;
    private Integer status;
    private List<MenuTreeVO> children = new ArrayList<>(); // 子节点存放处
}