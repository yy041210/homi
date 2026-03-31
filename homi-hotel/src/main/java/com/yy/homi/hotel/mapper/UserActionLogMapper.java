package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户行为日志 Mapper
 */
@Mapper
public interface UserActionLogMapper extends BaseMapper<UserActionLog> {
    /**
     * 根据酒店ID分组统计浏览次数 (VIEW_DETAIL)
     * 返回的 Map 包含：hotel_id 和 view_count
     */
    // 返回 List<Map>，每个 Map 代表一个酒店的统计结果
    List<Map<String, Object>> countViewsByHotelIds(@Param("hotelIds") List<String> hotelIds);

    @Select("select COUNT(*) from user_action_log where action_type = 'VIEW_DETAIL' AND user_id = #{userId}")
    int countViewByUserId(@Param("userId") String userId);
}