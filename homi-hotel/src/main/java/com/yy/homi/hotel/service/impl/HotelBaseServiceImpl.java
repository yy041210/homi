package com.yy.homi.hotel.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
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
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.AddressInfoTO;
import com.yy.homi.hotel.domain.convert.HotelConverter;
import com.yy.homi.hotel.domain.dto.request.HotelBasePageListReqDTO;
import com.yy.homi.hotel.domain.dto.request.HotelInsertDTO;
import com.yy.homi.hotel.domain.entity.*;
import com.yy.homi.hotel.domain.vo.HotelVO;
import com.yy.homi.hotel.feign.AmapLocationFeign;
import com.yy.homi.hotel.feign.SysCityFeign;
import com.yy.homi.hotel.feign.SysDistrictFeign;
import com.yy.homi.hotel.feign.SysProvinceFeign;
import com.yy.homi.hotel.mapper.HotelAlbumMapper;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelStatsMapper;
import com.yy.homi.hotel.service.*;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
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
    private HotelStatsMapper hotelStatsMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private HotelAlbumMapper hotelAlbumMapper;
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

                                    log.debug("第{}行 - 通过经纬度获取地区信息成功: 省={}, 市={}, 区={}",
                                            i, addressInfo.getProvince(), addressInfo.getCity(), addressInfo.getDistrict());
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

        if(CollectionUtil.isEmpty(hotelIds)){
            return R.ok(new ArrayList<>());
        }

        //查询省市区的名称
        R provinceNamesR = sysProvinceFeign.getNamesByIds(new ArrayList<>(provinceIds));
        R cityNamesR = sysCityFeign.getNamesByIds(new ArrayList<>(cityIds));
        R districtNamesR = sysDistrictFeign.getNamesByIds(new ArrayList<>(districtIds));

        if(provinceNamesR.getCode() != HttpStatus.OK.value() || cityNamesR.getCode() != HttpStatus.OK.value() || districtNamesR.getCode() != HttpStatus.OK.value() ){
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
            if(pId != null && provinceNameMap.get(pId) != null){
                hotelVO.setProvinceName(provinceNameMap.get(pId));
                regionPathList.add(provinceNameMap.get(pId));
            }
            if(cId != null && cityNameMap.get(cId) != null){
                hotelVO.setCityName(cityNameMap.get(cId));
                regionPathList.add(cityNameMap.get(cId));
            }
            if(dId != null && districtNameMap.get(dId) != null){
                hotelVO.setDistrictName(districtNameMap.get(dId));
                regionPathList.add(districtNameMap.get(dId));
            }
            String regionPath = StrUtil.join("/", regionPathList);
            hotelVO.setRegionPath(regionPath);

            //5张封面图
            hotelVO.setPicUrls(picMap.get(hotelId) == null ? new ArrayList<>() : picMap.get(hotelId));

            //hotelStats相关字段
            HotelStats hotelStats = statsMap.get(hotelId);
            if(hotelStats != null){
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

}