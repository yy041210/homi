package com.yy.homi.common.enums.hotel;

import lombok.Getter;

@Getter
public enum SurroundingCategoryEnum {

    OTHER(0,"其他"),
    TRAFFIC(1,"交通"),
    SCENICSPOT(2,"景点"),
    FOOD(3,"美食"),
    SHOP(4,"购物");




    private final int code;
    private final String desc;

    SurroundingCategoryEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据描述转换（
     */
    public static SurroundingCategoryEnum fromDesc(String desc) {
        for (SurroundingCategoryEnum s : values()) {
            if (s.desc.equals(desc)) return s;
        }
        return OTHER; // 默认值
    }
}
