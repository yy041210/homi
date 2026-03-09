package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HotelAlbumMapper extends BaseMapper<HotelAlbum> {

    /**
     * 批量查询酒店的封面五张图
     * 条件：酒店上传 (source=1), 公共区域 (roomId is null), 分类是精选 (category=1)
     */
    List<HotelAlbum> selectTop5PhotosBatch(@Param("hotelIds") List<String> hotelIds);

}
