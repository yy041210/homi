package com.yy.homi.hotel.service;

import com.yy.homi.common.domain.entity.R;

public interface AnalysisService {
    R hotRankList(String type,Integer pageSize);
}
