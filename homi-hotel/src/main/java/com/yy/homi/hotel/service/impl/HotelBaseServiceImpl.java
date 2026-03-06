package com.yy.homi.hotel.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.AddressInfoTO;
import com.yy.homi.hotel.domain.dto.request.HotelBasePageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.HotelStats;
import com.yy.homi.hotel.feign.AmapLocationFeign;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.mapper.HotelStatsMapper;
import com.yy.homi.hotel.service.HotelBaseService;
import com.yy.homi.hotel.service.HotelStatsService;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HotelBaseServiceImpl extends ServiceImpl<HotelBaseMapper, HotelBase> implements HotelBaseService {

    @Autowired
    private HotelStatsService hotelStatsService;
    @Autowired
    private HotelStatsMapper hotelStatsMapper;
    @Autowired
    private HotelBaseMapper hotelBaseMapper;
    @Autowired
    private AmapLocationFeign amapLocationFeign;

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
    public R selectHotelPage(HotelBasePageListReqDTO reqDTO) {
        // 1. 处理动态排序字符串 (防止 SQL 注入并处理驼峰转换)
        String orderByClause = "create_time DESC"; // 默认排序字段

        TableInfo tableInfo = TableInfoHelper.getTableInfo(HotelBase.class);
        if (StrUtil.isNotEmpty(reqDTO.getOrderByColumn()) && tableInfo != null) {
            // 将前端传来的字段名（可能是驼峰）转为数据库下划线列名
            String dbColumn = StrUtil.toUnderlineCase(reqDTO.getOrderByColumn());

            // 安全校验：判断该列是否存在于数据库表中
            boolean isValidField = tableInfo.getFieldList().stream()
                    .anyMatch(field -> field.getColumn().equals(dbColumn))
                    || "id".equals(dbColumn)
                    || "create_time".equals(dbColumn);

            if (isValidField) {
                // 组装 PageHelper 可识别的排序字符串，例如 "star ASC"
                orderByClause = dbColumn + (reqDTO.isAsc() ? " ASC" : " DESC");
            }
        }

        // 2. 开启分页拦截
        // PageHelper 会自动将 orderByClause 拼接到 SQL 的末尾
        PageHelper.startPage(reqDTO.getPageNum(), reqDTO.getPageSize(), orderByClause);

        // 3. 构造业务查询条件
        LambdaQueryWrapper<HotelBase> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询：酒店名称
        queryWrapper.like(StrUtil.isNotBlank(reqDTO.getName()), HotelBase::getName, reqDTO.getName());

        // 精确匹配：星级、省、市、区
        queryWrapper.eq(reqDTO.getStar() != null, HotelBase::getStar, reqDTO.getStar());
        queryWrapper.eq(reqDTO.getProvinceId() != null, HotelBase::getProvinceId, reqDTO.getProvinceId());
        queryWrapper.eq(reqDTO.getCityId() != null, HotelBase::getCityId, reqDTO.getCityId());
        queryWrapper.eq(reqDTO.getDistrictId() != null, HotelBase::getDistrictId, reqDTO.getDistrictId());

        // 4. 性能优化：排除大文本字段
        // 酒店简介 description 字段通常包含大量 HTML 内容，列表页不建议加载
        queryWrapper.select(HotelBase.class, info -> !info.getColumn().equals("description"));

        // 5. 执行查询
        // 注意：此时不需要在 queryWrapper 中调用 orderBy 方法，PageHelper 已经处理了排序
        List<HotelBase> list = this.list(queryWrapper);

        // 6. 封装并返回结果
        PageInfo<HotelBase> pageInfo = new PageInfo<>(list);
        return R.ok(pageInfo);
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

}