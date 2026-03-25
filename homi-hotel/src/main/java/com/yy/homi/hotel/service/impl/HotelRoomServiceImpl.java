package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.enums.hotel.AlbumCategoryEnum;
import com.yy.homi.common.enums.hotel.AlbumSourceEnum;
import com.yy.homi.hotel.domain.dto.request.HotelRoomPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelAlbum;
import com.yy.homi.hotel.domain.entity.HotelRoom;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelRoomMapper;
import com.yy.homi.hotel.service.HotelRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HotelRoomServiceImpl extends ServiceImpl<HotelRoomMapper, HotelRoom> implements HotelRoomService {

    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;
    @Autowired
    private HotelRoomMapper hotelRoomMapper;

    @Override
    public R pageList(HotelRoomPageListReqDTO reqDTO) {
        // 1. 开启分页 (必须放在查询执行之前)
        // 注意：PageHelper 拦截的是紧随其后的第一个 select 语句
        PageHelper.startPage(reqDTO.getPageNum(), reqDTO.getPageSize());

        // 2. 构造查询条件 (MyBatis Plus 的 LambdaQueryWrapper 依然可以生成 SQL)
        LambdaQueryWrapper<HotelRoom> lqw = new LambdaQueryWrapper<>();

        // 酒店、状态、人数等精确匹配
        lqw.eq(StrUtil.isNotBlank(reqDTO.getHotelId()), HotelRoom::getHotelId, reqDTO.getHotelId());
        lqw.eq(reqDTO.getStatus() != null, HotelRoom::getStatus, reqDTO.getStatus());
        lqw.eq(reqDTO.getMaxOccupancy() != null, HotelRoom::getMaxOccupancy, reqDTO.getMaxOccupancy());

        // 房型名称模糊查询
        lqw.like(StrUtil.isNotBlank(reqDTO.getName()), HotelRoom::getName, reqDTO.getName());

        // --- 补全面积查询逻辑 (核心：重叠区间匹配) ---
        // 房型最大面积 >= 用户要求的最小面积 AND 房型最小面积 <= 用户要求的最大面积
        lqw.ge(reqDTO.getMinArea() != null, HotelRoom::getMinArea, reqDTO.getMinArea());
        lqw.le(reqDTO.getMaxArea() != null, HotelRoom::getMaxArea, reqDTO.getMaxArea());

        // 床型模糊匹配（处理多关键词）
        if (StrUtil.isNotBlank(reqDTO.getBedType())) {
            String bedType = reqDTO.getBedType().trim();
            String[] keywords = bedType.split("\\s+");
            lqw.and(w -> {
                for (String key : keywords) {
                    w.like(HotelRoom::getBedType, key);
                }
            });
        }

        // 时间范围过滤
        lqw.ge(reqDTO.getBeginTime() != null, HotelRoom::getCreateTime, reqDTO.getBeginTime());
        lqw.le(reqDTO.getEndTime() != null, HotelRoom::getCreateTime, reqDTO.getEndTime());

        // 排序
        lqw.orderByDesc(HotelRoom::getCreateTime);

        // 3. 执行查询 (此时 BaseMapper.selectList 会被 PageHelper 拦截并自动拼接 limit)
        List<HotelRoom> list = this.list(lqw);

        // 4.查询房型的图集( roomId + 酒店上传 + 分类：房型  )
        Set<String> roomIds = list.stream().map(HotelRoom::getId).collect(Collectors.toSet());
        Map<String, List<String>> roomIdImageUrlsMap = hotelAlbumMapper
                .selectList(
                        new LambdaQueryWrapper<HotelAlbum>()
                                .eq(HotelAlbum::getSource, AlbumSourceEnum.HOTEL.getCode())
                                .eq(HotelAlbum::getCategory, AlbumCategoryEnum.ROOM.getCode())
                                .in(CollectionUtil.isNotEmpty(roomIds), HotelAlbum::getRoomId, roomIds)
                                .orderByAsc(HotelAlbum::getSeq)
                )
                .stream()
                .collect(Collectors.groupingBy(
                        HotelAlbum::getRoomId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                hotelAlbumList -> hotelAlbumList.stream().limit(5).map(HotelAlbum::getImageUrl).collect(Collectors.toList())
                        )
                ));


        //5.分装结果
        List<JSONObject> resultList = list.stream().map(room -> {
            //转为JSONObject
            JSONObject jsonObject = (JSONObject) JSON.toJSON(room);

            String roomId = room.getId();
            List<String> imageUrls = roomIdImageUrlsMap.get(roomId);
            if (imageUrls != null) {
                jsonObject.put("imageUrls", imageUrls);
            } else {
                jsonObject.put("imageUrls", new ArrayList<>());
            }

            return jsonObject;
        }).collect(Collectors.toList());


        // 6. 使用 PageInfo 包装结果 (包含 total, pages, records 等)
        PageInfo<JSONObject> pageInfo = new PageInfo<>(resultList);

        // 7. 返回统一结果对象
        return R.ok(pageInfo);
    }

    @Override
    public R deleteById(String id) {
        if (StrUtil.isBlank(id)) {
            return R.fail("房型id不能为空！");
        }
        //todo 删除房型
        return R.ok("删除成功！");
    }

    @Override
    public R deleteByIds(List<String> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return R.ok("删除成功！");
        }
        //todo 批量删除房型
        return R.ok("删除成功！");
    }

    @Override
    public R changeStatus(String id) {
        if (StrUtil.isBlank(id)) {
            return R.fail("房型id不能为空！");
        }

        HotelRoom hotelRoom = hotelRoomMapper.selectById(id);
        if(hotelRoom == null){
            return R.fail("房型id对应房型不存在！");
        }

        Integer status = hotelRoom.getStatus();
        Integer newStatus = 0;
        if(status == newStatus){
            newStatus = 1;
            hotelRoomMapper.changeStatus(id,newStatus);
            return R.ok("禁用成功！");
        }else {
            hotelRoomMapper.changeStatus(id,newStatus);
            return R.ok("启用成功！");
        }

    }
}
