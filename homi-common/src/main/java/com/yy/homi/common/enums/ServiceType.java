package com.yy.homi.common.enums;

import lombok.Getter;

/**
 * 微服务业务类型枚举
 */
@Getter
public enum ServiceType {

    // 定义实例
    RBAC("homi-rbac", "权限管理中心"),
    FILE("homi-file", "文件上传服务"),
    HOTEL("homi-hotel", "酒店业务服务"),
    GATEWAY("homi-gateway","网关服务");

    // 定义私有属性
    private final String serviceId;
    private final String description;

    //构造函数：必须将参数赋值给成员变量
    ServiceType(String serviceId, String description) {
        this.serviceId = serviceId;
        this.description = description;
    }

}