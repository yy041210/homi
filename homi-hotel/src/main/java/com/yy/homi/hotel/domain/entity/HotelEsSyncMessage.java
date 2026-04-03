package com.yy.homi.hotel.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//酒店数据与es同步消息
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotelEsSyncMessage {

    public static final String SYNC_ONLY_TYPE ="SYNC_ONLY";
    public static final String SYNC_BATCH_TYPE ="SYNC_BATCH";
    public static final String DELETE_ONLY_TYPE ="DELETE_ONLY";
    public static final String DELETE_BATCH_TYPE ="DELETE_BATCH";

    private String businessType;  //业务类型：SYNC_ONLY(同步单个)，SYNC_BATCH(同步一批),DELETE_ONLY,DELETE_BATCH

    private Object data;


}
