package com.yy.homi.rbac.importer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yy.homi.rbac.domain.entity.SysProvince;
import com.yy.homi.rbac.domain.entity.SysCity;
import com.yy.homi.rbac.domain.entity.SysDistrict;
import com.yy.homi.rbac.service.SysProvinceService;
import com.yy.homi.rbac.service.SysCityService;
import com.yy.homi.rbac.service.SysDistrictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;

//通过调用高德地图api，初始化省，市，县/区表数据
@Slf4j
@Component
@RequiredArgsConstructor
public class AmapDistrictImporter {

    private final RestTemplate restTemplate;
    private final SysProvinceService provinceService;
    private final SysCityService cityService;
    private final SysDistrictService districtService;

    // TODO: 请将这里替换为你自己在高德开放平台申请的、有效且已配置好IP白名单的API Key
    private static final String AMAP_KEY = "4f643f23940ff69096c6c6754bb52281";
    private static final String AMAP_DISTRICT_URL = "https://restapi.amap.com/v3/config/district";

    // 启动时是否自动导入，建议第一次手动调用，或通过Controller触发
//     @PostConstruct
    public void init() {
        log.info("开始从高德地图导入行政区划数据...");
        this.importChinaDistricts();
    }

    /**
     * 公开的导入方法，可以被Controller调用
     */
    @Transactional(rollbackFor = Exception.class)
    public void importChinaDistricts() {
        // 1. 构建请求URL并调用API
        String url = UriComponentsBuilder.fromHttpUrl(AMAP_DISTRICT_URL)
                .queryParam("key", AMAP_KEY)
                .queryParam("keywords", "中国")
                .queryParam("subdistrict", "3")  // 获取省、市、区三级
                .queryParam("extensions", "base") // 只需要基础信息，不需要边界坐标
                .build()
                .toUriString();

        log.info("请求高德API URL: {}", url);
        String responseStr = restTemplate.getForObject(url, String.class);
        log.debug("API原始响应: {}", responseStr);

        // 2. 解析响应
        JSONObject root = JSONObject.parseObject(responseStr);
        String status = root.getString("status");

        if (!"1".equals(status)) {
            String info = root.getString("info");
            log.error("API调用失败，状态码: {}, 信息: {}", status, info);
            throw new RuntimeException("高德API调用失败: " + info);
        }

        // 3. 获取中国顶级区域下的省份列表
        JSONArray districts = root.getJSONArray("districts");
        if (districts == null || districts.isEmpty()) {
            log.warn("API返回的districts为空");
            return;
        }

        // 中国的顶级行政区划是第一个元素
        JSONObject china = districts.getJSONObject(0);
        JSONArray provinces = china.getJSONArray("districts");

        int provinceCount = 0, cityCount = 0, districtCount = 0;

        // 4. 遍历所有省份
        for (int i = 0; i < provinces.size(); i++) {
            JSONObject provinceJson = provinces.getJSONObject(i);
            if (!"province".equals(provinceJson.getString("level"))) {
                continue;
            }

            // --- 保存省份 ---
            SysProvince province = new SysProvince();
            province.setId(Integer.valueOf(provinceJson.getString("adcode")));
            province.setName(provinceJson.getString("name"));
            // 解析中心点坐标
            String center = provinceJson.getString("center");
            if (center != null && center.contains(",")) {
                String[] lngLat = center.split(",");
                province.setCenterLng(new BigDecimal(lngLat[0]));
                province.setCenterLat(new BigDecimal(lngLat[1]));
            }
            province.setSort(i);
            province.setStatus(1);
            provinceService.saveOrUpdate(province);
            provinceCount++;
            log.debug("已处理省份：{}", province.getName());

            // 遍历该省份下的城市
            JSONArray cities = provinceJson.getJSONArray("districts");
            if (cities == null) continue;

            for (int j = 0; j < cities.size(); j++) {
                JSONObject cityJson = cities.getJSONObject(j);
                // 高德返回的level可能是"city"，也可能是"province"（直辖市）
                String cityLevel = cityJson.getString("level");
                if (!"city".equals(cityLevel) && !"province".equals(cityLevel)) {
                    continue;
                }

                // --- 保存城市 ---
                SysCity city = new SysCity();
                city.setId(Integer.valueOf(cityJson.getString("adcode")));
                city.setName(cityJson.getString("name"));
                city.setProvinceId(province.getId());

                String cityCenter = cityJson.getString("center");
                if (cityCenter != null && cityCenter.contains(",")) {
                    String[] lngLat = cityCenter.split(",");
                    city.setCenterLng(new BigDecimal(lngLat[0]));
                    city.setCenterLat(new BigDecimal(lngLat[1]));
                }
                city.setSort(j);
                city.setStatus(1);
                cityService.saveOrUpdate(city);
                cityCount++;
                log.debug("  已处理城市：{}", city.getName());

                // 遍历该城市下的区县
                JSONArray districtsArray = cityJson.getJSONArray("districts");
                if (districtsArray == null) continue;

                for (int k = 0; k < districtsArray.size(); k++) {
                    JSONObject districtJson = districtsArray.getJSONObject(k);
                    if (!"district".equals(districtJson.getString("level"))) {
                        continue;
                    }

                    // --- 保存区县 ---
                    SysDistrict district = new SysDistrict();
                    district.setId(Integer.valueOf(districtJson.getString("adcode")));
                    district.setName(districtJson.getString("name"));
                    district.setCityId(city.getId());

                    String districtCenter = districtJson.getString("center");
                    if (districtCenter != null && districtCenter.contains(",")) {
                        String[] lngLat = districtCenter.split(",");
                        district.setCenterLng(new BigDecimal(lngLat[0]));
                        district.setCenterLat(new BigDecimal(lngLat[1]));
                    }
                    district.setSort(k);
                    district.setStatus(1);
                    districtService.saveOrUpdate(district);
                    districtCount++;
                }
            }
        }

        log.info("数据导入完成！共处理：{} 个省 / 直辖市, {} 个市, {} 个区/县", 
            provinceCount, cityCount, districtCount);
    }
}