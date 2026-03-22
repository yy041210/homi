package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.hotel.domain.entity.HotelComment;
import com.yy.homi.hotel.mapper.HotelCommentMapper;
import com.yy.homi.hotel.service.HotelCommentService;
import org.springframework.stereotype.Service;

@Service
public class HotelCommentServiceImpl extends ServiceImpl<HotelCommentMapper, HotelComment> implements HotelCommentService {
}
