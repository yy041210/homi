package com.yy.homi.rbac.service;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.RegionInsertReqDTO;

public interface SysRegionService {
    R getRegionTree();

    R searchRegionTree(String keyword);

    R insertRegion(RegionInsertReqDTO reqDTO);
}
