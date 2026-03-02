package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import org.springframework.web.multipart.MultipartFile;

public interface HotelAlbumService extends IService<HotelAlbum> {
    R importHotelAlbumFromCsv(MultipartFile file);
}
