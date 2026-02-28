package com.yy.homi.thirdparty.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.AddressInfoTO;
import com.yy.homi.thirdparty.feigns.RegionFeign;
import com.yy.homi.thirdparty.service.AmapLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AmapLocationServiceImpl implements AmapLocationService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RegionFeign regionFeign;

    @Value("${amap.key}")
    private String amapKey;

    @Override
    public R getAddressByLngLat(Double lng, Double lat) {
        return getAddressByLngLat(lng, lat, "GCJ02");
    }

    @Override
    public R getAddressByLngLat(Double lng, Double lat, String coordType) {
        AddressInfoTO result = new AddressInfoTO();
        result.setLng(lng);
        result.setLat(lat);

        try {
            // 构建请求URL（高德使用GCJ02坐标系）
            String url = "https://restapi.amap.com/v3/geocode/regeo" + "?key=" + amapKey
                    + "&location=" + lng + "," + lat
                    + "&extensions=base";

            log.debug("请求高德地图API: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseAddressInfo(response.getBody(), lng, lat);
            } else {
                return R.fail("调用高德API失败");
            }
        } catch (Exception e) {
            log.error("获取地址信息异常", e);
            return R.fail("获取地址信息异常!");
        }
    }

    /**
     * 解析高德返回的JSON数据
     */
    private R parseAddressInfo(String json, Double lng, Double lat) {
        AddressInfoTO info = new AddressInfoTO();
        info.setLng(lng);
        info.setLat(lat);

        try {
            JSONObject obj = JSONObject.parseObject(json);
            String status = obj.getString("status");

            if (!"1".equals(status)) {
                String info_code = obj.getString("info");
                log.warn("高德API返回错误: {}", info_code);
                return R.fail("高德API错误：" + info_code);
            }

            JSONObject regeo = obj.getJSONObject("regeocode");
            if (regeo == null) {
                return R.fail("未找到地址信息");
            }

            JSONObject addressComp = regeo.getJSONObject("addressComponent");
            String province = addressComp.getString("province");
            String city = addressComp.getString("city");
            String district = addressComp.getString("district");

            info.setProvince(province);
            info.setCity(city);
            info.setDistrict(district);
            this.setProCityDisIdByName(info,province,city,district);
            info.setTownship(addressComp.getString("township"));
            info.setFormattedAddress(regeo.getString("formatted_address"));
            log.debug("解析到地址信息: {}", info);

        } catch (Exception e) {
            log.error("解析高德返回数据异常", e);
            return R.fail("解析异常：" + e.getMessage());
        }
        return R.ok(info);
    }

    public void setProCityDisIdByName(AddressInfoTO addressInfoTO, String provinceName, String cityName, String districtName) {
        if (addressInfoTO != null ){

            //获取省id
            if(!StrUtil.isEmpty(provinceName)){
                R rProvince = regionFeign.getIdByProvinceName(provinceName);
                if(rProvince.getCode() == HttpStatus.OK.value()){
                    int proId = Integer.parseInt(rProvince.getData().toString());
                    addressInfoTO.setProvinceId(proId);
                    //获取市id
                    if(!StrUtil.isEmpty(cityName) ){
                        R rCity = regionFeign.getIdByCityNameAndProvinceId(cityName, proId);
                        if(rCity.getCode() == HttpStatus.OK.value()){
                            int cityId = Integer.parseInt(rCity.getData().toString());
                            addressInfoTO.setCityId(cityId);

                            //获取区域id
                            if(!StrUtil.isEmpty(districtName)){
                                R rDistrict = regionFeign.getIdByDistrictNameAndCityId(districtName, cityId);
                                if(rDistrict.getCode() == HttpStatus.OK.value()){
                                    int districtId = Integer.parseInt(rDistrict.getData().toString());
                                    addressInfoTO.setDistrictId(districtId);
                                }
                            }

                        }

                    }
                }
            }

        }
    }
}
