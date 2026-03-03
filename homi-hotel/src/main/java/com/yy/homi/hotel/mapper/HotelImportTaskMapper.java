package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface HotelImportTaskMapper extends BaseMapper<HotelImportTask> {

    void incrementProcessedCount(@Param("taskId") String taskId, @Param("increment") int increment);

    @Update("update hotel_import_task set status = #{status}, update_time = NOW() where id = #{taskId} ")
    void updateStatusById(@Param("taskId") String taskId, @Param("status") int status);

    @Update("update hotel_import_task set finish_time = #{date}, update_time = #{finishTime} , costTime = #{costTime} - start_time where id = #{taskId} ")
    void updateFinishTime(@Param("taskId") String taskId,@Param("date") Date finishTime,@Param("costTime") Long costTime);

    // 批量插入
    int insertBatch(@Param("list") List<HotelImportTask> list);

    // 查询
    HotelImportTask selectById(@Param("id") String id);
    List<HotelImportTask> selectAll();
    List<HotelImportTask> selectByStatus(@Param("status") Integer status);
    List<HotelImportTask> selectByCreateBy(@Param("createBy") String createBy);
    List<HotelImportTask> selectPage(@Param("taskName") String taskName,
                                     @Param("taskType") String taskType,
                                     @Param("status") Integer status,
                                     @Param("createBy") String createBy);

    // 更新状态
    int updateToRunning(@Param("id") String id, @Param("status") Integer status);
    int updateToSuccess(@Param("id") String id, @Param("status") Integer status);
    int updateToFailed(@Param("id") String id, @Param("status") Integer status,
                       @Param("errorMsg") String errorMsg);

    // 进度更新
    int updateTotalCount(@Param("id") String id, @Param("totalCount") Integer totalCount);

    // 删除
    int deleteById(@Param("id") String id);
    int deleteBatch(@Param("list") List<String> idList);

    // 统计
    List<HotelImportTask> selectRunningTasks();
    List<HotelImportTask> selectRecentTasks(@Param("limit") Integer limit);

    // 更新
    int updateTask(HotelImportTask task);
}