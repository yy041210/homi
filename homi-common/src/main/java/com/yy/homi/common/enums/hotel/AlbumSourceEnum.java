package com.yy.homi.common.enums.hotel;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AlbumSourceEnum {
    HOTEL(1, "酒店上传"),
    USER(2, "用户上传");

    private final int code;
    private final String desc;

    AlbumSourceEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据描述转换（用于导入 CSV）
     */
    public static AlbumSourceEnum fromDesc(String desc) {
        for (AlbumSourceEnum s : values()) {
            if (s.desc.equals(desc)) return s;
        }
        return HOTEL; // 默认值
    }

}
