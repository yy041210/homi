package com.yy.homi.common.domain.to;

import lombok.Data;
import lombok.ToString;
import java.io.Serializable;

/**
 * 地址信息TO
 */
@Data
@ToString
public class AddressInfoTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 省份ID
     */
    private Integer provinceId;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 城市ID
     */
    private Integer cityId;
    
    /**
     * 区/县
     */
    private String district;
    
    /**
     * 区/县ID
     */
    private Integer districtId;
    
    /**
     * 街道
     */
    private String township;
    
    /**
     * 格式化地址
     */
    private String formattedAddress;
    
    /**
     * 原始经纬度（经度）
     */
    private Double lng;
    
    /**
     * 原始经纬度（纬度）
     */
    private Double lat;

}