package com.yy.homi.hotel.domain.convert;

import com.yy.homi.hotel.domain.dto.request.UserActionLogInsertReqDTO;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserActionLogConverter {

    UserActionLog insertReqDtoToEntity(UserActionLogInsertReqDTO userActionLogInsertReqDTO);

}
