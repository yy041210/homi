package com.yy.homi.thirdparty.service;

import com.yy.homi.common.domain.entity.R;

public interface AmapLocationService {
    R getAddressByLngLat(Double lng, Double lat);

    R getAddressByLngLat(Double lng, Double lat, String coordType);
}
