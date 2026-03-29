package com.yy.homi.hotel.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.stream.SimpleCollector;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.AddressInfoTO;
import com.yy.homi.common.enums.hotel.AlbumCategoryEnum;
import com.yy.homi.common.enums.hotel.AlbumSourceEnum;
import com.yy.homi.common.enums.hotel.SurroundingCategoryEnum;
import com.yy.homi.hotel.domain.convert.HotelConverter;
import com.yy.homi.hotel.domain.dto.request.HotelBasePageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelDocPageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelInsertDTO;
import com.yy.homi.hotel.domain.entity.*;
import com.yy.homi.hotel.domain.vo.HotelVO;
import com.yy.homi.hotel.feign.AmapLocationFeign;
import com.yy.homi.hotel.feign.SysCityFeign;
import com.yy.homi.hotel.feign.SysDistrictFeign;
import com.yy.homi.hotel.feign.SysProvinceFeign;
import com.yy.homi.hotel.mapper.*;
import com.yy.homi.hotel.service.*;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Cardinality;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HotelBaseServiceImpl extends ServiceImpl<HotelBaseMapper, HotelBase> implements HotelBaseService {

    @Autowired
    private HotelStatsService hotelStatsService;
    @Autowired
    private HotelRoomService hotelRoomService;
    @Autowired
    private HotelFacilityService hotelFacilityService;
    @Autowired
    private HotelAlbumService hotelAlbumService;
    @Autowired
    private HotelSurroundingService hotelSurroundingService;
    @Autowired
    private HotelRoomFacilityMapper hotelRoomFacilityMapper;
    @Autowired
    private HotelFacilityMapper hotelFacilityMapper;
    @Autowired
    private HotelRoomMapper hotelRoomMapper;
    @Autowired
    private HotelStatsMapper hotelStatsMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;
    @Autowired
    private HotelFacilityTypeMapper hotelFacilityTypeMapper;
    @Autowired
    private AmapLocationFeign amapLocationFeign;
    @Autowired
    private SysProvinceFeign sysProvinceFeign;
    @Autowired
    private SysCityFeign sysCityFeign;
    @Autowired
    private SysDistrictFeign sysDistrictFeign;
    @Autowired
    private HotelConverter hotelConverter;

    @Autowired
    private RestHighLevelClient client;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R importHotelBaseFromJsonCsv(MultipartFile file) {
        // 1. 基础校验
        if (file.isEmpty()) return R.fail("文件不能为空！");

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            return R.fail("格式错误：仅支持 .csv 文件");
        }

        try (InputStream is = file.getInputStream()) {
            // 2. 读取数据 (doReadSync 适合中小规模数据)
            List<Map<Integer, String>> list = EasyExcel.read(is).sheet().doReadSync();
            if (list == null || list.size() <= 1) return R.fail("文件内容为空或缺少数据行");

            List<HotelBase> hotelList = new ArrayList<>();
            List<HotelStats> hotelStatsList = new ArrayList<>();
            // 用于统计
            int successCount = 0;
            int failCount = 0;

            // 3. 循环解析 (从第1行开始，跳过第0行表头)
            for (int i = 1; i < list.size(); i++) {
                Map<Integer, String> row = list.get(i);
                try {
                    String hotelId = row.get(0);

                    String nameInfoJson = row.get(1);
                    String commentInfoJson = row.get(4);
                    String positionInfoJson = row.get(5);

                    // 解析 JSON 对象
                    JSONObject nameObj = JSON.parseObject(nameInfoJson);
                    JSONObject posObj = JSON.parseObject(positionInfoJson);
                    JSONObject commentObj = JSON.parseObject(commentInfoJson);

                    // 组装 HotelBase
                    HotelBase hotel = new HotelBase();
                    hotel.setId(hotelId);
                    hotel.setStatus(CommonConstants.STATUS_ENABLED);
                    hotel.setName(nameObj.getString("name"));
                    hotel.setNameEn(nameObj.getString("enName"));
                    hotel.setStar(Integer.valueOf(row.get(3))); // CSV 第4列是 star
                    hotel.setAddress(posObj.getString("address"));

                    //组装hotelStats
                    HotelStats hotelStats = new HotelStats();
                    hotelStats.setHotelId(hotelId);
                    hotelStats.setCommentScore(commentObj.getFloat("commentScore"));
                    String commonCountStr = commentObj.getString("commenterNumber").replaceAll("条点评", "").replaceAll(",", "");
                    hotelStats.setCommentCount(StrUtil.isEmpty(commonCountStr) ? null : Integer.parseInt(commonCountStr));
                    if (commentObj.containsKey("subScore") && commentObj.getJSONArray("subScore").size() > 0) {
                        JSONArray subScore = commentObj.getJSONArray("subScore");
                        hotelStats.setHygieneScore(subScore.getJSONObject(0).getFloat("number"));
                        hotelStats.setDeviceScore(subScore.getJSONObject(1).getFloat("number"));
                        hotelStats.setEnvironmentScore(subScore.getJSONObject(2).getFloat("number"));
                        hotelStats.setServiceScore(subScore.getJSONObject(3).getFloat("number"));

                    }
                    if (commentObj.containsKey("oneSentenceComment") && commentObj.getJSONArray("oneSentenceComment").size() > 0) {
                        JSONArray oneSentenceComment = commentObj.getJSONArray("oneSentenceComment");
                        String tagTitle = oneSentenceComment.getJSONObject(0).getString("tagTitle");
                        hotelStats.setTagTitle(tagTitle);
                    }
                    hotelStatsList.add(hotelStats);

                    // 获取经纬度
                    Double lat = null;
                    Double lng = null;
                    var coords = posObj.getJSONArray("mapCoordinate");
                    if (coords != null && !coords.isEmpty()) {
                        lat = coords.getJSONObject(0).getDouble("latitude");
                        lng = coords.getJSONObject(0).getDouble("longitude");
                        hotel.setLat(lat);
                        hotel.setLng(lng);

                        // 调用远程服务获取地址信息
                        if (lat != null && lng != null) {
                            try {
                                // 远程调用获取地址信息
                                R r = amapLocationFeign.getAddressByLngLat(lng, lat);
                                Thread.sleep(300);
                                if (r != null && r.getCode() == 200 && r.getData() != null) {
                                    AddressInfoTO addressInfo = JSON.parseObject(JSON.toJSONString(r.getData()), AddressInfoTO.class);

                                    // 设置地区ID
                                    hotel.setProvinceId(addressInfo.getProvinceId());
                                    hotel.setCityId(addressInfo.getCityId());
                                    hotel.setDistrictId(addressInfo.getDistrictId());

                                    // 设置冗余字段（方便查询）
//                                    hotel.setProvinceName(addressInfo.getProvince());
//                                    hotel.setCityName(addressInfo.getCity());
                                    // hotel.setDistrictName(addressInfo.getDistrict());

                                    log.debug("第{}行 - 通过经纬度获取地区信息成功: 省={}, 市={}, 区={}", i, addressInfo.getProvince(), addressInfo.getCity(), addressInfo.getDistrict());
                                }
                            } catch (Exception e) {
                                log.error("第{}行 - 调用地区服务异常，HotelBase: {} - 异常信息：{}", i, hotel, e.getMessage());
                            }
                        }
                        //没有经纬度无需设置
                    }
                    // 没有坐标信息，无需设置
                    hotelList.add(hotel);
                    successCount++;

                } catch (Exception e) {
                    log.error("第{}行解析失败，跳过。错误：{}", i, e.getMessage());
                    failCount++;
                }
            }

            // 4. 过滤已存在的酒店和重复ID
            Set<String> existHotelBaseIds = hotelBaseMapper.selectList(null)
                    .stream()
                    .map(HotelBase::getId)
                    .collect(Collectors.toSet());

            // 用于去重
            Set<String> idRecords = new HashSet<>();

            List<HotelBase> newHotelList = hotelList
                    .stream()
                    .filter(hotelBase -> !existHotelBaseIds.contains(hotelBase.getId()))
                    .filter(hotelBase -> {
                        if (!idRecords.contains(hotelBase.getId())) {
                            idRecords.add(hotelBase.getId());
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            // 5. 批量保存
            if (!newHotelList.isEmpty()) {
                this.saveOrUpdateBatch(newHotelList);
            }

            if (CollectionUtil.isNotEmpty(hotelStatsList)) {
                hotelStatsService.saveOrUpdateBatch(hotelStatsList);
            }


            return R.ok(String.format("导入完成：总行数%d，成功%d条，失败%d条，新增%d条",
                    list.size() - 1, successCount, failCount, newHotelList.size()));

        } catch (Exception e) {
            log.error("文件导入异常", e);
            return R.fail("导入失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询酒店列表
     */
    @Override
    public R selectHotelPage(HotelBasePageListReqDTO reqDTO) {
        Integer pageNum = reqDTO.getPageNum();
        Integer pageSize = reqDTO.getPageSize();
        String name = reqDTO.getName();
        Integer star = reqDTO.getStar();
        Integer status = reqDTO.getStatus();
        Integer provinceId = reqDTO.getProvinceId();
        Integer cityId = reqDTO.getCityId();
        Integer districtId = reqDTO.getDistrictId();
        Date beginTime = reqDTO.getBeginTime();
        Date endTime = reqDTO.getEndTime();

        PageHelper.startPage(pageNum, pageSize);

        List<HotelBase> hotelBases = hotelBaseMapper.selectHotelList(name, star, status, provinceId, cityId, districtId, beginTime, endTime);
        PageInfo<HotelBase> basePageInfo = new PageInfo<>(hotelBases);

        List<HotelVO> hotelVOS = hotelConverter.listEntityToVo(hotelBases);

        //收集酒店ids和省市区ids
        Set<String> hotelIds = hotelVOS.stream().map(HotelVO::getId).collect(Collectors.toSet());
        Set<Integer> provinceIds = hotelVOS.stream().map(HotelVO::getProvinceId).collect(Collectors.toSet());
        Set<Integer> cityIds = hotelVOS.stream().map(HotelVO::getCityId).collect(Collectors.toSet());
        Set<Integer> districtIds = hotelVOS.stream().map(HotelVO::getDistrictId).collect(Collectors.toSet());

        if (CollectionUtil.isEmpty(hotelIds)) {
            return R.ok(new ArrayList<>());
        }

        //查询省市区的名称
        R provinceNamesR = sysProvinceFeign.getNamesByIds(new ArrayList<>(provinceIds));
        R cityNamesR = sysCityFeign.getNamesByIds(new ArrayList<>(cityIds));
        R districtNamesR = sysDistrictFeign.getNamesByIds(new ArrayList<>(districtIds));

        if (provinceNamesR.getCode() != HttpStatus.OK.value() || cityNamesR.getCode() != HttpStatus.OK.value() || districtNamesR.getCode() != HttpStatus.OK.value()) {
            return R.fail("远程调用查询地址信息错误!");
        }
        // 这样转换后，Map 里的 Key 就真的变成 Integer 了
        Map<Integer, String> provinceNameMap = Convert.toMap(Integer.class, String.class, provinceNamesR.getData());
        Map<Integer, String> cityNameMap = Convert.toMap(Integer.class, String.class, cityNamesR.getData());
        Map<Integer, String> districtNameMap = Convert.toMap(Integer.class, String.class, districtNamesR.getData());


        //查询酒店5张封面图
        List<HotelAlbum> hotelAlbums = hotelAlbumMapper.selectTop5PhotosBatch(new ArrayList<>(hotelIds));
        Map<String, List<String>> picMap = hotelAlbums.stream()
                .collect(Collectors.groupingBy(
                        HotelAlbum::getHotelId,
                        Collectors.mapping(HotelAlbum::getImageUrl, Collectors.toList())
                ));


        //查询关联的hotelStats
        List<HotelStats> hotelStatsList = hotelStatsMapper.selectList(new LambdaQueryWrapper<HotelStats>().in(HotelStats::getHotelId, hotelIds));
        Map<String, HotelStats> statsMap = CollStreamUtil.toIdentityMap(hotelStatsList, HotelStats::getHotelId);

        //封装返回数据
        hotelVOS.forEach(hotelVO -> {
            String hotelId = hotelVO.getId();
            Integer pId = hotelVO.getProvinceId();
            Integer cId = hotelVO.getCityId();
            Integer dId = hotelVO.getDistrictId();
            //省市区名
            List<String> regionPathList = new ArrayList<>();
            if (pId != null && provinceNameMap.get(pId) != null) {
                hotelVO.setProvinceName(provinceNameMap.get(pId));
                regionPathList.add(provinceNameMap.get(pId));
            }
            if (cId != null && cityNameMap.get(cId) != null) {
                hotelVO.setCityName(cityNameMap.get(cId));
                regionPathList.add(cityNameMap.get(cId));
            }
            if (dId != null && districtNameMap.get(dId) != null) {
                hotelVO.setDistrictName(districtNameMap.get(dId));
                regionPathList.add(districtNameMap.get(dId));
            }
            String regionPath = StrUtil.join("/", regionPathList);
            hotelVO.setRegionPath(regionPath);

            //5张封面图
            hotelVO.setPicUrls(picMap.get(hotelId) == null ? new ArrayList<>() : picMap.get(hotelId));

            //hotelStats相关字段
            HotelStats hotelStats = statsMap.get(hotelId);
            if (hotelStats != null) {
                hotelVO.setMinPrice(hotelStats.getMinPrice());
                hotelVO.setCommentScore(hotelStats.getCommentScore());
                hotelVO.setCommentCount(hotelStats.getCommentCount());
                hotelVO.setCommentDescription(hotelStats.getCommentDescription());
                hotelVO.setTagTitle(hotelStats.getTagTitle());
                hotelVO.setHygieneScore(hotelStats.getHygieneScore());
                hotelVO.setEnvironmentScore(hotelStats.getEnvironmentScore());
                hotelVO.setServiceScore(hotelStats.getServiceScore());
                hotelVO.setDeviceScore(hotelStats.getDeviceScore());
            }

        });

        // 4. 创建最终的 VO 分页对象，并从 basePageInfo 复制分页元数据
        PageInfo<HotelVO> hotelVOPageInfo = new PageInfo<>();
        BeanUtil.copyProperties(basePageInfo, hotelVOPageInfo, "list"); // 拷贝分页属性，排除旧的 list
        hotelVOPageInfo.setList(hotelVOS); // 设置填充好数据的 VO 列表
        return R.ok(hotelVOPageInfo);
    }


    @Override
    public R getByDistrictId(Integer districtId) {
        if (districtId == null) {
            return R.fail("districtId不能为空！");
        }
        List<HotelBase> hotelBases = hotelBaseMapper.selectList(new LambdaQueryWrapper<HotelBase>().eq(HotelBase::getDistrictId, districtId));
        return R.ok(hotelBases);
    }


    @Override
    public R getByCityId(Integer cityId) {
        if (cityId == null) {
            return R.fail("districtId不能为空！");
        }
        List<HotelBase> hotelBases = hotelBaseMapper.selectList(new LambdaQueryWrapper<HotelBase>().eq(HotelBase::getCityId, cityId));
        return R.ok(hotelBases);
    }

    @Override
    public R getByProvinceId(Integer provinceId) {
        if (provinceId == null) {
            return R.fail("districtId不能为空！");
        }
        List<HotelBase> hotelBases = hotelBaseMapper.selectList(new LambdaQueryWrapper<HotelBase>().eq(HotelBase::getProvinceId, provinceId));
        return R.ok(hotelBases);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R saveHotel(HotelInsertDTO req) {
        // ================== 1. 手动参数校验逻辑 ==================
        // 校验基础信息
        HotelBase base = req.getBaseInfo();
        if (base == null) {
            return R.fail("酒店基础信息不能为空");
        }
        if (StrUtil.isBlank(base.getName())) {
            return R.fail("酒店名称不能为空");
        }
        if (base.getStar() == null || base.getStar() < 1 || base.getStar() > 5) {
            return R.fail("酒店星级必须在1-5之间");
        }
        if (StrUtil.isBlank(base.getAddress())) {
            return R.fail("酒店地址不能为空");
        }

        // 校验房型信息
        List<HotelRoom> rooms = req.getRooms();
        if (CollectionUtil.isEmpty(rooms)) {
            return R.fail("至少需要录入一个房型");
        }
        for (HotelRoom room : rooms) {
            if (StrUtil.isBlank(room.getName())) {
                return R.fail("房型名称不能为空");
            }
            if (room.getMaxOccupancy() == null || room.getMaxOccupancy() <= 0) {
                return R.fail("房型[" + room.getName() + "]的最大入住人数必须大于0");
            }
        }

        // 校验相册信息 (可选)
        List<HotelAlbum> albums = req.getAlbums();
        if (CollectionUtil.isNotEmpty(albums)) {
            for (int i = 0; i < albums.size(); i++) {
                if (StrUtil.isBlank(albums.get(i).getImageUrl())) {
                    return R.fail("第" + (i + 1) + "张图片地址不能为空");
                }
            }
        }

        //  2. 核心保存逻辑
        // 保存酒店基础信息 (获取生成的ID)
        this.save(base);
        String hotelId = base.getId();

        // 保存统计信息
        HotelStats stats = req.getStats();
        if (stats != null) {
            stats.setHotelId(hotelId);
            hotelStatsMapper.insert(stats);
        }

        // 批量保存房型
        rooms.forEach(r -> r.setHotelId(hotelId));
        hotelRoomService.saveBatch(rooms);

        // 批量保存设施
        List<HotelFacility> facilities = req.getFacilities();
        if (CollectionUtil.isNotEmpty(facilities)) {
            facilities.forEach(f -> f.setHotelId(hotelId));
            hotelFacilityService.saveBatch(facilities);
        }

        // 批量保存相册
        if (CollectionUtil.isNotEmpty(albums)) {
            albums.forEach(a -> a.setHotelId(hotelId));
            hotelAlbumService.saveBatch(albums);
        }

        // 批量保存周边
        List<HotelSurrounding> surroundings = req.getSurroundings();
        if (CollectionUtil.isNotEmpty(surroundings)) {
            surroundings.forEach(s -> s.setHotelId(hotelId));
            hotelSurroundingService.saveBatch(surroundings);
        }

        return R.ok("保存成功！");
    }

    @Override
    public R getInfoById(String id) {
        if (StrUtil.isEmpty(id)) {
            return R.fail("酒店id不能为空！");
        }

        //1.查询酒店基本信息
        HotelBase hotelBase = this.getById(id);
        if (hotelBase == null) {
            return R.fail("酒店id对应酒店不存在！");
        }


        JSONObject result = new JSONObject();
        result.put("id", id);
        result.put("name", hotelBase.getName());
        result.put("nameEn", hotelBase.getNameEn());
        result.put("star", hotelBase.getStar());
        result.put("openYear", hotelBase.getOpenYear());
        result.put("roomCount", hotelBase.getRoomCount());
        result.put("description", hotelBase.getDescription());
        result.put("provinceId", hotelBase.getProvinceId());
        result.put("cityId", hotelBase.getCityId());
        result.put("districtId", hotelBase.getDistrictId());
        result.put("address", hotelBase.getAddress());
        result.put("lat", hotelBase.getLat());
        result.put("lng", hotelBase.getLng());


        //2.查询HotelStats信息
        HotelStats hotelStats = hotelStatsMapper.selectOne(new LambdaQueryWrapper<HotelStats>().eq(HotelStats::getHotelId, id));
        if (hotelStats != null) {
            result.put("commentCount", hotelStats.getCommentCount());
            result.put("commentDesc", hotelStats.getCommentDescription());
            result.put("commentScore", hotelStats.getCommentScore());
            result.put("hygieneScore", hotelStats.getHygieneScore());
            result.put("deviceScore", hotelStats.getDeviceScore());
            result.put("environmentScore", hotelStats.getEnvironmentScore());
            result.put("serviceScore", hotelStats.getServiceScore());
            result.put("tagTitle", hotelStats.getTagTitle());

        }

        //3.查询酒店房型
        List<HotelRoom> hotelRooms = hotelRoomService.list(new LambdaQueryWrapper<HotelRoom>().eq(HotelRoom::getHotelId, id));
        if (CollectionUtil.isNotEmpty(hotelRooms)) {
            Set<String> roomIds = CollStreamUtil.toSet(hotelRooms, HotelRoom::getId);
            //查询每个房型的3张图片
            //酒店上传 + 精选 + 对应的roomId和hotelId
            Map<String, List<String>> idImageUrlsMap = hotelAlbumMapper.selectList(new LambdaQueryWrapper<HotelAlbum>()
                    .eq(HotelAlbum::getHotelId, id)
                    .eq(HotelAlbum::getSource, AlbumSourceEnum.HOTEL.getCode())
                    .eq(HotelAlbum::getCategory, AlbumCategoryEnum.ROOM.getCode())
                    .in(HotelAlbum::getRoomId, roomIds)
            ).stream().collect(Collectors.groupingBy(
                    HotelAlbum::getRoomId, // Key: 房间ID
                    Collectors.mapping(HotelAlbum::getImageUrl, Collectors.toList()) // Value: 只要URL并转成List
            ));

            //todo房型设备列表

            JSONArray hotelRoomJsonList = new JSONArray();
            for (HotelRoom hotelRoom : hotelRooms) {
                JSONObject jsonObject = new JSONObject();
                String roomId = hotelRoom.getId();
                jsonObject.put("roomId", roomId);
                jsonObject.put("name", hotelRoom.getName());
                jsonObject.put("area", hotelRoom.getArea());
                jsonObject.put("floor", hotelRoom.getFloor());
                jsonObject.put("bedType", hotelRoom.getBedType());
                jsonObject.put("window", hotelRoom.getWindow());
                jsonObject.put("wifi", hotelRoom.getWifi());
                jsonObject.put("smoke", hotelRoom.getSmoke());
                jsonObject.put("maxOccupancy", hotelRoom.getMaxOccupancy());
                jsonObject.put("highlightFields", hotelRoom.getHighlightFields());
                jsonObject.put("status", hotelRoom.getStatus());
                List<String> imageUrls = idImageUrlsMap.get(roomId);
                jsonObject.put("imageUrls", imageUrls);
                hotelRoomJsonList.add(jsonObject);
            }
            result.put("hotelRooms", hotelRoomJsonList);

        }


        //4.查询酒店设备
        List<HotelFacility> hotelFacilities = hotelFacilityService.list(
                new LambdaQueryWrapper<HotelFacility>()
                        .eq(HotelFacility::getHotelId, id)
                        .orderByAsc(HotelFacility::getSeq)
        );
        if (CollectionUtil.isNotEmpty(hotelFacilities)) {
            Set<String> typeIds = hotelFacilities.stream().map(HotelFacility::getHotelFacilityTypeId).collect(Collectors.toSet());

            Map<String, List<HotelFacility>> typeIdHotelFacilityMap = hotelFacilities.stream().collect(Collectors.groupingBy(HotelFacility::getHotelFacilityTypeId));

            List<HotelFacilityType> hotelFacilityTypes = hotelFacilityTypeMapper.selectList(new LambdaQueryWrapper<HotelFacilityType>()
                    .in(HotelFacilityType::getId, typeIds)
                    .orderByAsc(HotelFacilityType::getSeq)
            );

            JSONArray hotelFacilityListJson = new JSONArray();
            for (HotelFacilityType hotelFacilityType : hotelFacilityTypes) {
                String typeId = hotelFacilityType.getId();
                ArrayList<HotelFacility> facilityList = (ArrayList<HotelFacility>) typeIdHotelFacilityMap.get(typeId);
                if (CollectionUtil.isEmpty(facilityList)) {
                    continue;
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("facilityTypeId", typeId);
                jsonObject.put("facilityTypeName", hotelFacilityType.getName());
                jsonObject.put("facilityTypeIcon", hotelFacilityType.getIcon());

                JSONArray hotelFacilitiesJSON = new JSONArray();
                for (HotelFacility hotelFacility : facilityList) {
                    JSONObject hotelFacilityJson = new JSONObject();
                    hotelFacilityJson.put("facilityId", hotelFacility.getId());
                    hotelFacilityJson.put("facilityName", hotelFacility.getName());
                    hotelFacilityJson.put("facilityIcon", hotelFacility.getIcon());
                    hotelFacilityJson.put("status", hotelFacility.getStatus());
                    List<String> tagList = hotelFacility.getTagList();
                    hotelFacilityJson.put("tags", tagList);
                    if (StrUtil.isNotBlank(hotelFacility.getImageUrl())) {
                        hotelFacilityJson.put("imageUrl", hotelFacility.getImageUrl());
                    }
                    hotelFacilitiesJSON.add(hotelFacilityJson);
                }
                jsonObject.put("facilities", hotelFacilitiesJSON);
                hotelFacilityListJson.add(jsonObject);
            }
            result.put("hotelFacility", hotelFacilityListJson);
        }

        //5.查询酒店周边
        List<HotelSurrounding> hotelSurroundings = hotelSurroundingService.findByHotelId(id); //排好序了
        // 获取所有枚举值并按 code 属性升序排列
        SurroundingCategoryEnum[] categories = SurroundingCategoryEnum.values();
        Arrays.sort(categories, Comparator.comparingInt(SurroundingCategoryEnum::getCode));

        JSONArray surroundings = new JSONArray();
        for (SurroundingCategoryEnum surroundingCategoryEnum : categories) {
            JSONObject surroundingCategoryJSON = new JSONObject();
            // 这里的循环就是按 code 0, 1, 2, 3... 顺序执行了
            int code = surroundingCategoryEnum.getCode();
            String desc = surroundingCategoryEnum.getDesc();
            surroundingCategoryJSON.put("surroundingCategory", desc);

            ArrayList<Object> surroundingJsonList = new ArrayList<>();
            List<HotelSurrounding> hotelSurroundingList = hotelSurroundings.stream().filter(item -> item.getCategory() == code)
                    .collect(Collectors.toList());
            if (CollectionUtil.isEmpty(hotelSurroundingList)) {
                continue;
            }
            for (HotelSurrounding hotelSurrounding : hotelSurroundingList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("surroundingId", hotelSurrounding.getId());
                jsonObject.put("surroundingName", hotelSurrounding.getSurroundingName());
                jsonObject.put("distance", hotelSurrounding.getDistance());
                jsonObject.put("distanceDesc", hotelSurrounding.getDistanceDesc());
                jsonObject.put("arrivalType", hotelSurrounding.getArrivalType());
                jsonObject.put("tagName", hotelSurrounding.getTagName());
                jsonObject.put("lat", hotelSurrounding.getLat());
                jsonObject.put("lon", hotelSurrounding.getLon());
                surroundingJsonList.add(jsonObject);
            }
            surroundingCategoryJSON.put("surroundings", surroundingJsonList);

            surroundings.add(surroundingCategoryJSON);
        }
        result.put("surrounding", surroundings);


        //6.酒店图集7张点击在查询更多
        //酒店上传 + 精选 + 前七张
        List<String> imageUrls = hotelAlbumMapper.selectList(new LambdaQueryWrapper<HotelAlbum>()
                .eq(HotelAlbum::getHotelId, id)
                .eq(HotelAlbum::getSource, AlbumSourceEnum.HOTEL.getCode())
                .eq(HotelAlbum::getCategory, AlbumCategoryEnum.FEATURED.getCode())
                .orderByAsc(HotelAlbum::getSeq)
                .last("limit 7")
        ).stream().map(HotelAlbum::getImageUrl).collect(Collectors.toList());
        result.put("imageUrls", imageUrls);

        return R.ok(result);
    }

    @Override
    public R changeStatus(String id) {
        if (StrUtil.isBlank(id)) {
            return R.fail("酒店id不能为空！");
        }

        HotelBase hotelBase = hotelBaseMapper.selectOne(new LambdaQueryWrapper<HotelBase>()
                .eq(HotelBase::getId, id));
        if (hotelBase == null) {
            return R.fail("酒店id不存在！");
        }

        Integer status = hotelBase.getStatus();
        int newStatus = 0;
        if (status == CommonConstants.STATUS_ENABLED) {
            newStatus = 1;
            hotelBaseMapper.changeStatus(id, newStatus);
            return R.ok("禁用成功！");
        } else {
            hotelBaseMapper.changeStatus(id, newStatus);
            return R.ok("启用成功！");
        }
    }


    @Override
    public R searchPageList(HotelDocPageListReqDTO params) {
        try {
            // 1. 准备 Request (索引名: hoteldoc)
            SearchRequest request = new SearchRequest("hoteldoc");

            // 2. 构造查询主体
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // --- 2.1 关键字搜索 (all 字段) ---
            if (StrUtil.isBlank(params.getKey())) {
                boolQuery.must(QueryBuilders.matchAllQuery());
            } else {
                // 使用 matchQuery 匹配万能字段 all
                boolQuery.must(QueryBuilders.matchQuery("all", params.getKey()));
            }

            // --- 2.2 基础条件过滤 (Filter 提高性能) ---
            // 位置过滤
            if (params.getProvinceId() != null)
                boolQuery.filter(QueryBuilders.termQuery("provinceId", params.getProvinceId()));
            if (params.getCityId() != null) boolQuery.filter(QueryBuilders.termQuery("cityId", params.getCityId()));
            if (params.getDistrictId() != null)
                boolQuery.filter(QueryBuilders.termQuery("districtId", params.getDistrictId()));

            // 星级过滤
            if (params.getStar() != null) boolQuery.filter(QueryBuilders.termQuery("star", params.getStar()));

            // 价格区间 (注意：HotelDoc 中字段名为 price)
            if (params.getMinPrice() != null || params.getMaxPrice() != null) {
                RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
                if (params.getMinPrice() != null) priceRange.gte(params.getMinPrice());
                if (params.getMaxPrice() != null) priceRange.lte(params.getMaxPrice());
                boolQuery.filter(priceRange);
            }

            // 评分过滤
            if (params.getMinScore() != null) {
                boolQuery.filter(QueryBuilders.rangeQuery("commentScore").gte(params.getMinScore()));
            }

            // 面积过滤 (对应 HotelDoc 中的 maxArea)
            if (params.getMinRoomSize() != null) {
                boolQuery.filter(QueryBuilders.rangeQuery("maxArea").gte(params.getMinRoomSize()));
            }

            // 设施多选过滤 (使用 .raw 后缀)
            if (CollUtil.isNotEmpty(params.getFacilities())) {
                for (String facility : params.getFacilities()) {
                    boolQuery.filter(QueryBuilders.termQuery("facilities.raw", facility));
                }
            }

            // 房型设施多选过滤 (使用 .raw 后缀)
            if (CollUtil.isNotEmpty(params.getRoomFacilities())) {
                for (String rFacility : params.getRoomFacilities()) {
                    boolQuery.filter(QueryBuilders.termQuery("roomFacilities.raw", rFacility));
                }
            }

            // 距离半径过滤 (如果传了位置和半径)
            if (StrUtil.isNotBlank(params.getLocation()) && params.getRangeRadius() != null) {
                boolQuery.filter(QueryBuilders.geoDistanceQuery("location")
                        .point(new GeoPoint(params.getLocation()))
                        .distance(params.getRangeRadius(), DistanceUnit.KILOMETERS));
            }

            // --- 2.3 权重控制 (Function Score) ---
            // 基于 popularityScore 提升排名
            FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                    boolQuery,
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                            new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                    ScoreFunctionBuilders.weightFactorFunction(2) // 默认基础权重
                            ),
                            // 如果有广告逻辑可以加在这里
                    });

            request.source().query(functionScoreQuery);

            // --- 2.4 核心：按酒店折叠 (Collapse) ---
            // 房型扁平化后，搜索结果一个酒店只显示一条，默认展示最匹配(或价格最低)的房型
            request.source().collapse(new CollapseBuilder("hotelId"));

            // --- 2.5 分页与排序 ---
            request.source().from(params.getFrom()).size(params.getSize());

            String sortBy = params.getSortBy();
            if ("priceAsc".equals(sortBy)) {
                request.source().sort("price", SortOrder.ASC);
            } else if ("priceDesc".equals(sortBy)) {
                request.source().sort("price", SortOrder.DESC);
            } else if ("commentScore".equals(sortBy)) {
                request.source().sort("commentScore", SortOrder.DESC);
            } else if ("commentCount".equals(sortBy)) {
                request.source().sort("commentCount", SortOrder.DESC);
            } else if (StrUtil.isNotBlank(params.getLocation()) && "distance".equals(sortBy)) {
                // 地理距离排序
                String loc = params.getLocation();
                double lat = Double.parseDouble(loc.split(",")[0]);
                double lon = Double.parseDouble(loc.split(",")[1]);
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", lat, lon)
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS));
            } else {
                // 默认按相关性得分及权重排序
                request.source().sort(SortBuilders.scoreSort().order(SortOrder.DESC));
            }

            // 使用 Cardinality 聚合来统计有多少个唯一的 hotelId
            request.source().aggregation(
                    AggregationBuilders.cardinality("hotel_count").field("hotelId")
            );

            // 3. 执行搜索
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            // 4. 解析结果
            return handleResponse(response);

        } catch (Exception e) {
            log.error("酒店搜索异常", e);
            return R.fail("酒店搜索服务不可用");
        }
    }

    @Override
    public R suggestion(String key) {
        if (StrUtil.isBlank(key)) {
            return R.fail("关键词不能为空！");
        }

        //1.准备请求
        SearchRequest request = new SearchRequest("hoteldoc");
        //2.准备DSL语句
        request.source().suggest(new SuggestBuilder()
                .addSuggestion(
                        "suggestions",
                        SuggestBuilders.completionSuggestion("suggestion")
                                .prefix(key)
                                .skipDuplicates(true)
                                .size(20)
                )
        );
        //3.发送请求
        SearchResponse response = null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            return R.fail("es查询失败！");
        }
        //解析结果
        List<String> result = new ArrayList<>();
        Suggest suggest = response.getSuggest();
        CompletionSuggestion suggestion = suggest.getSuggestion("suggestions");
        List<CompletionSuggestion.Entry.Option> options = suggestion.getOptions();
        for (CompletionSuggestion.Entry.Option option : options) {
            String text = option.getText().toString();
            result.add(text);
        }

        return R.ok(result);
    }

    @Override
    public R getHotelFacilityFilters() {
        try {
            SearchRequest request = new SearchRequest("hoteldoc");

            // 1. 全量查询：统计索引库中所有文档
            request.source().query(QueryBuilders.matchAllQuery());

            // 2. 性能优化：只要聚合统计结果，不需要文档详情
            request.source().size(0);

            // 3. 核心聚合：按原始值（Keyword）统计
            // 酒店设施聚合
            request.source().aggregation(
                    AggregationBuilders.terms("hotel_facilities_agg")
                            .field("facilities.raw")
                            .size(200) // 设施可能较多，给大一点的桶容量
            );

            // 房型设施聚合
            request.source().aggregation(
                    AggregationBuilders.terms("room_facilities_agg")
                            .field("roomFacilities.raw")
                            .size(200)
            );

            // 4. 执行 ES 请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            // 5. 解析聚合结果
            Aggregations aggs = response.getAggregations();
            Map<String, List<String>> filterMap = new HashMap<>();

            // 提取酒店设施
            filterMap.put("hotelFacilities", getKeysFromAgg(aggs.get("hotel_facilities_agg")));
            // 提取房型设施
            filterMap.put("roomFacilities", getKeysFromAgg(aggs.get("room_facilities_agg")));

            return R.ok(filterMap);

        } catch (IOException e) {
            log.error("获取酒店设施全量过滤器失败", e);
            return R.fail("获取筛选器失败");
        }
    }

    @Override
    public R syncHotelDocFromDB() {
        //查询所有房型
        List<HotelRoom> hotelRooms = hotelRoomMapper.selectList(null);
        Set<String> roomIds = hotelRooms.stream().map(HotelRoom::getId).collect(Collectors.toSet());
        Set<String> hotelIds = hotelRooms.stream().map(HotelRoom::getHotelId).collect(Collectors.toSet());

        //查询所有HotelBase
        List<HotelBase> hotelBases = hotelBaseMapper.selectBatchIds(hotelIds);
        Map<String, HotelBase> baseIdentityMap = CollStreamUtil.toIdentityMap(hotelBases, HotelBase::getId);

        //查询HotelStats
        List<HotelStats> hotelStatsList = hotelStatsMapper.selectList(new LambdaQueryWrapper<HotelStats>().in(HotelStats::getHotelId, hotelIds));
        Map<String, HotelStats> statsIdentityMap = CollStreamUtil.toIdentityMap(hotelStatsList, HotelStats::getHotelId);

        //查询HotelFacilities
        List<HotelFacility> facilityList = hotelFacilityMapper.selectList(new LambdaQueryWrapper<HotelFacility>().in(HotelFacility::getHotelId, hotelIds));
        Map<String, Set<String>> hotelIdFacilityNamesMap = facilityList.stream().collect(Collectors.groupingBy(
                HotelFacility::getHotelId,
                Collectors.mapping(HotelFacility::getName, Collectors.toSet())
        ));

        //查询HotelRoomFacilities
        List<HotelRoomFacility> hotelRoomFacilityList = hotelRoomFacilityMapper.selectList(new LambdaQueryWrapper<HotelRoomFacility>().in(HotelRoomFacility::getRoomId, roomIds));
        Map<String, Set<String>> hotelIdRoomFacilityNamesMap = hotelRoomFacilityList.stream().collect(Collectors.groupingBy(
                HotelRoomFacility::getRoomId,
                Collectors.mapping(
                        HotelRoomFacility::getName,
                        Collectors.toSet()
                )
        ));

        //查询酒店前5张图
        List<HotelAlbum> hotelAlbums = hotelAlbumMapper.selectTop5PhotosBatch(new ArrayList<>(hotelIds));
        Map<String, List<String>> hotelIdImageUrlsMap = hotelAlbums.stream().collect(Collectors.groupingBy(
                HotelAlbum::getHotelId,
                Collectors.mapping(
                        HotelAlbum::getImageUrl,
                        Collectors.toList()
                )
        ));


        //循环每一个房型
        List<HotelDoc> saveHotelDocs = new ArrayList<>();
        for (HotelRoom hotelRoom : hotelRooms) {
            String roomId = hotelRoom.getId();
            String hotelId = hotelRoom.getHotelId();
            if (StrUtil.isBlank(roomId) || StrUtil.isBlank(hotelId)) {
                continue;
            }

            //HotelBase
            HotelBase hotelBase = baseIdentityMap.get(hotelId);
            if (hotelBase == null) {
                continue;
            }

            //HotelStats
            HotelStats hotelStats = statsIdentityMap.get(hotelId);

            //HotelFacilities
            Set<String> hotelFacilities = hotelIdFacilityNamesMap.get(hotelId);

            //HotelRoomFacilities
            Set<String> hotelRoomFacilities = hotelIdRoomFacilityNamesMap.get(roomId);

            HotelDoc hotelDoc = HotelDoc.build(
                    hotelRoom,
                    hotelBase,
                    hotelStats,
                    CollectionUtil.isEmpty(hotelFacilities) ? new ArrayList<>() : new ArrayList<>(hotelFacilities),
                    CollectionUtil.isEmpty(hotelRoomFacilities) ? new ArrayList<>() : new ArrayList<>(hotelRoomFacilities)
            );

            //picUrls
            if (hotelIdImageUrlsMap.get(hotelId) != null) {
                hotelDoc.setPicUrls(hotelIdImageUrlsMap.get(hotelId));
            }

            saveHotelDocs.add(hotelDoc);
        }

        //批量保存hotelDoc
        if (CollUtil.isEmpty(saveHotelDocs)) {
            return R.ok("没有可同步的数据");
        }

        try {
            // 定义每批次提交的数量（建议 500-1000 条）
            int batchSize = 1000;
            int total = saveHotelDocs.size();

            for (int i = 0; i < total; i += batchSize) {
                // 1. 创建 BulkRequest，指定索引名
                BulkRequest bulkRequest = new BulkRequest("hoteldoc");

                // 获取当前分片的数据
                int end = Math.min(i + batchSize, total);
                List<HotelDoc> subList = saveHotelDocs.subList(i, end);

                for (HotelDoc doc : subList) {
                    // 2. 将 HotelDoc 转换为 JSON 并添加到请求中
                    // 注意：必须指定 ID（房型ID），否则 ES 会自动生成随机 ID 导致无法幂等更新
                    bulkRequest.add(new IndexRequest()
                            .id(doc.getId())
                            .source(JSON.toJSONString(doc), XContentType.JSON));
                }

                // 3. 执行同步发送
                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);

                // 4. 检查结果
                if (bulkResponse.hasFailures()) {
                    log.error("ES 批量同步存在失败条目，详情: {}", bulkResponse.buildFailureMessage());
                    // 根据业务逻辑，可以选择继续或者抛出异常
                }

                log.info("ES 酒店文档同步中：已完成 {}/{}", end, total);
            }

            return R.ok("同步成功，共计 " + total + " 条房型文档");

        } catch (IOException e) {
            log.error("ES 批量保存发生 IO 异常", e);
            return R.fail("同步失败，网络或 ES 服务异常");
        } catch (Exception e) {
            log.error("ES 同步发生未知异常", e);
            return R.fail("同步失败：" + e.getMessage());
        }

    }

    /**
     * 提取 Bucket 中的 Key 值并转为 List
     */
    private List<String> getKeysFromAgg(Terms terms) {
        if (terms == null) return Collections.emptyList();
        return terms.getBuckets().stream()
                .map(Terms.Bucket::getKeyAsString)
                .collect(Collectors.toList());
    }

    private R handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 获取总条数
        Cardinality hotelCountAgg = response.getAggregations().get("hotel_count");
        long realTotal = hotelCountAgg.getValue(); // 这里拿到的才是折叠后的 文档数

        List<HotelDoc> list = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 获取文档内容 JSON
            String json = hit.getSourceAsString();
            // 反序列化为对象 (建议使用 FastJSON 或 Hutool JSON)
            HotelDoc doc = JSON.parseObject(json, HotelDoc.class);

            // 获取排序值 (如果有地理位置排序，第一个排序值即为距离)
            Object[] sortValues = hit.getSortValues();
            if (sortValues != null && sortValues.length > 0) {
                // 距离通常是 Double 类型
                doc.setDistance((Double) sortValues[0]);
            }
            list.add(doc);
        }

        // 组装无泛型 R 结果
        Map<String, Object> result = new HashMap<>();
        result.put("total", realTotal);
        result.put("list", list);

        return R.ok(result);
    }

}