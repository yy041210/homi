package com.yy.homi.rbac.service;

import com.yy.homi.common.domain.entity.R;

public interface AuthService {
    R logout(String userId);
}
