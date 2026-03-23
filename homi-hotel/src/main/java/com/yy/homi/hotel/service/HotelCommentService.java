package com.yy.homi.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelCommentPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelComment;

public interface HotelCommentService extends IService<HotelComment> {
    R pageList(HotelCommentPageListReqDTO reqDTO);

}
