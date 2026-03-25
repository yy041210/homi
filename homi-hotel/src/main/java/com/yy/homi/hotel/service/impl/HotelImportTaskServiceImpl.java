package com.yy.homi.hotel.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.utils.IdUtils;
import com.yy.homi.hotel.domain.dto.request.HotelImportTaskPageListReqDTO;
import com.yy.homi.hotel.domain.entity.HotelImportTask;
import com.yy.homi.hotel.mapper.HotelImportTaskMapper;
import com.yy.homi.hotel.service.HotelImportTaskService;
import com.yy.homi.hotel.strategy.context.HotelImportTaskStrategyContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class HotelImportTaskServiceImpl extends ServiceImpl<HotelImportTaskMapper, HotelImportTask> implements HotelImportTaskService {

    @Autowired
    private HotelImportTaskStrategyContext hotelImportTaskStrategyContext;
    @Autowired
    private HotelImportTaskMapper hotelImportTaskMapper;

    @Override
    public R insertHotelImportTask(String taskName, String taskType, MultipartFile file) {
        //参数校验
        if (StrUtil.isEmpty(taskType)) {
            return R.fail("任务类型不能为空！");
        }
        if (file == null || file.isEmpty()) {
            return R.fail("上传文件不能为空！");
        }
        String fileName = file.getOriginalFilename();
        if (StrUtil.isEmpty(fileName)) {
            return R.fail("文件名非法！");
        }
        // 校验是否为 csv 或 excel
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!suffix.equals("csv")) {
            return R.fail("仅支持 CSV 文件上传！");
        }
        //判断策略是否存在
        if (!hotelImportTaskStrategyContext.existStrategyType(taskType)) {
            //不存在
            return R.fail("任务类型对应策略不存在！");
        }

        //将文件保存到临时目录
        String fixedTempDir = "D:\\ideaProjects\\homi\\temp\\";
        //String fixedTempDir = "/home/homi/temp/import"; //linux

        // 确保目录存在
        File dir = new File(fixedTempDir);
        if (!dir.exists()) {
            dir.mkdirs(); //不存在就创建
        }

        // 4. 生成唯一文件名，防止多人上传同名文件冲突
        String snowflakeId = String.valueOf(IdUtils.getSnowflakeId());
        String saveName = "HOMI_" + taskType + "_" + snowflakeId;
        String realFilePath = fixedTempDir + File.separator + saveName;
        File destFile = new File(realFilePath);
        // 保存文件到磁盘
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("文件导入任务创建失败，磁盘IO异常", e);
            return R.fail("服务器文件存储失败，请联系管理员");
        }

        long lineCount = FileUtil.getTotalLines(destFile);
        if ((lineCount - 1) <= 0) {
            destFile.delete();
            return R.fail("csv文件中无数据！");
        } else {
            //判断csv格式
            // 1. 创建 CsvReader（指定 GBK 编码解决你之前的报错）
            CsvReader reader = CsvUtil.getReader();
            CsvData gbk = reader.read(destFile, Charset.forName("GBK"));

            if (taskType.equals("HOTEL_FACILITY")) {
                //酒店设备，7列
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 7) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            } else if (taskType.equals("HOTEL_ROOM")) {
                //酒店基本房型，12列
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 12) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            } else if (taskType.equals("HOTEL_INTRODUCTION")) {
                //酒店简介相关内容，5列
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 5) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            } else if (taskType.equals("HOTEL_COMMENT")) {
                //酒店评论相关内容，12列
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 12) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            } else if (taskType.equals("HOTEL_ROOM_FACILITY")) {
                //酒店房型关联设备
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 7) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            } else if (taskType.equals("HOTEL_BASE")) {
                //酒店基本信息 15列
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 15) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            } else if (taskType.equals("HOTEL_ALBUM")) {
                //酒店图集信息 6列
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 6) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            } else if (taskType.equals("HOTEL_SURROUNDING")) {
                //酒店周边信息 8列
                CsvData csvData = gbk;
                if (csvData.getRowCount() > 0) {
                    int columnCount = csvData.getRow(0).getFieldCount();
                    if (columnCount != 8) {
                        destFile.delete();
                        return R.fail("csv文件格式不正确！");
                    }
                }
            }

        }

        // 5. 创建导入任务记录并入库
        HotelImportTask task = new HotelImportTask();
        task.setTaskName(StrUtil.isEmpty(taskName) ? taskType + "_" + "任务_" + snowflakeId : taskName);
        task.setTaskType(taskType);
        task.setStatus(HotelImportTask.STATUS_WAITING);
        task.setTotalCount((int) lineCount - 2);  //表头不算,末尾换行
        task.setProcessedCount(0);
        hotelImportTaskMapper.insert(task);

        //6异步插入数据
        String taskId = task.getId();
        hotelImportTaskStrategyContext.execute(taskId, realFilePath, taskType);

        return R.ok(taskId);
    }

    @Override
    public R pageList(HotelImportTaskPageListReqDTO req) {
        // 1. 开启分页 (PageHelper 必须在查询前调用)
        PageHelper.startPage(req.getPageNo(), req.getPageSize());

        // 2. 调用 Mapper，传入离散参数
        List<HotelImportTask> list = hotelImportTaskMapper
                .selectTaskList(
                        req.getTaskName(),
                        req.getTaskType(),
                        req.getStatus(),
                        req.getBeginTime(),
                        req.getEndTime()
                );

        // 3. 包装原始分页结果（为了获取 Total 等信息）
        PageInfo<HotelImportTask> pageInfo = new PageInfo<>(list);

        // 返回无泛型的 R
        return R.ok(pageInfo);
    }

    @Override
    public R getImportTaskByIds(List<String> ids) {
        // 集合为空直接返回
        if (CollUtil.isEmpty(ids)) {
            return R.ok(new ArrayList<HotelImportTask>());
        }

        List<HotelImportTask> taskList = hotelImportTaskMapper.selectImportTaskByIds(ids);

        return R.ok(taskList);
    }
}
