package com.yy.homi.rbac.service;

import com.yy.homi.common.domain.entity.R;

public interface SysRegionService {
    R getRegionTree();

    R searchRegionTree(String keyword);

}
