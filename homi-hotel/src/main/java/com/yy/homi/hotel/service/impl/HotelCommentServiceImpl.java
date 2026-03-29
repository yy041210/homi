package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.HotelCommentPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelComment;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelCommentMapper;
import com.yy.homi.hotel.service.HotelCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HotelCommentServiceImpl extends ServiceImpl<HotelCommentMapper, HotelComment> implements HotelCommentService {

    @Autowired
    private HotelCommentMapper hotelCommentMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;

    @Override
    public R pageList(HotelCommentPageListReqDTO reqDTO) {
        PageHelper.startPage(reqDTO.getPageNum(), reqDTO.getPageSize());

        String hotelId = reqDTO.getHotelId();
        String roomId = reqDTO.getRoomId();
        String travelType = reqDTO.getTravelType();
        Integer sortType = reqDTO.getSortType();

        LambdaQueryWrapper<HotelComment> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(hotelId)) {
            queryWrapper.eq(HotelComment::getHotelId, hotelId);

        }
        if (StrUtil.isNotBlank(roomId)) {
            queryWrapper.eq(HotelComment::getRoomId, roomId);
        }

        if (StrUtil.isNotBlank(travelType)) {
            queryWrapper.eq(HotelComment::getTravelType, travelType);
        }

        if (reqDTO.getBeginTime() != null && reqDTO.getEndTime() != null) {
            queryWrapper.between(HotelComment::getPublishTime, reqDTO.getBeginTime(), reqDTO.getEndTime());
        }

        //处理排序逻辑
        switch (sortType) {
            case 1:
                queryWrapper.orderByDesc(HotelComment::getCommentScore); // 高分优先
                break;
            case 2:
                queryWrapper.orderByDesc(HotelComment::getLikeCount);    // 点赞优先
                break;
            default:
                queryWrapper.orderByDesc(HotelComment::getPublishTime);  // 时间降序(默认)
                break;
        }

        List<HotelComment> hotelComments = hotelCommentMapper.selectList(queryWrapper);

        //查询关联的文件
        Set<String> commentIds = hotelComments.stream().map(HotelComment::getId).collect(Collectors.toSet());

        List<HotelAlbum> hotelAlbums = hotelAlbumMapper
                .selectList(
                        new LambdaQueryWrapper<HotelAlbum>()
                                .in(HotelAlbum::getCommentId, commentIds)
                                .orderByAsc(HotelAlbum::getSeq)
        );

        if(CollectionUtil.isNotEmpty(hotelAlbums)){
            Map<String, List<String>> commentIdUrlsMap = CollStreamUtil.groupBy(
                    hotelAlbums,
                    HotelAlbum::getCommentId,
                    Collectors.mapping(HotelAlbum::getImageUrl, Collectors.toList()));

            hotelComments.forEach(hotelComment -> {
                if(commentIdUrlsMap.get(hotelComment.getId()) != null){
                    hotelComment.setFileUrls(commentIdUrlsMap.get(hotelComment.getId()));
                }
            });

        }
        return R.ok(new PageInfo<>(hotelComments));
    }
}
