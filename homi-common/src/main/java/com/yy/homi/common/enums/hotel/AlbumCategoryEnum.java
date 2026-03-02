package com.yy.homi.common.enums.hotel;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AlbumCategoryEnum {
    FEATURED(1, "精选"),
    APPEARANCE(2, "外观"),
    ROOM(3, "房间"),
    CATERING(4, "餐饮"),
    LEISURE(5, "休闲"),
    BUSINESS(6, "商务"),
    PUBLICAREA(7, "公共区域"),
    SURROUNDING(8, "周边"),
    OTHER(9, "其他");

    private final int code;
    private final String desc;

    AlbumCategoryEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AlbumCategoryEnum fromDesc(String desc) {
        for (AlbumCategoryEnum c : values()) {
            if (c.desc.equals(desc)) return c;
        }
        return OTHER;
    }
}