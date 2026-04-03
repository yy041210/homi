package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
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

    @Select("select star,COUNT(*) as count from user_action_log where user_id =#{userId} AND action_type= 'VIEW_DETAIL' group by star")
    List<Map<String, Object>> countViewHotelStarByUserId(@Param("userId") String userId);

    @Select("select city_id,COUNT(*) as count from user_action_log where user_id =#{userId} AND action_type= 'VIEW_DETAIL' group by city_id order by count desc")
    List<Map<String, Object>> countViewCityByUserId(@Param("userId") String userId);

    List<Map<String, Object>> countViewByHour(@Param("userId") String userId);

    List<Map<String, Object>> countPriceRangePreference(@Param("userId") String userId);

    List<Map<String, Object>> countViewCommentScoreRange(@Param("userId") String userId);

    List<Map<String, Object>> countTrendByDate(@Param("userId") String userId, @Param("beginTime") Date beginTime, @Param("endTime") Date endTime);


    List<Map<String, Object>> countActionTypeByUserId(@Param("userId") String userId);

    List<Double> getPricesByScoreRange(@Param("minScore") Double minScore, @Param("maxScore") Double maxScore, @Param("userId") String userId);

    List<Map<String, Object>> countViewCommentCountRange(@Param("userId") String userId);

    Map<String, Object> getUserRadarStats(@Param("userId") String userId);

    List<Map<String, Object>> countOpenYear(@Param("userId") String userId);

    List<Map<String, Object>> selectRoomCountByUserId(@Param("userId") String userId);

    List<String> selectFacilityNamesByUserId(@Param("userId") String userId);

    List<UserActionLog> selectAreaAndActionList(@Param("userId") String userId);
}