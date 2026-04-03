package com.yy.homi.hotel.task;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.hotel.domain.entity.SparkTask;
import com.yy.homi.hotel.mapper.SparkTaskMapper;
import org.apache.spark.SparkConf;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.storage.StorageLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;


import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

@Component
public class ScheduledTask {

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    @Value("${spring.redis.port:6379}")
    private int redisPort;
    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SparkTaskMapper sparkTaskMapper;

    @Scheduled(cron = "0 0 3 ? * MON")
    public void calculateHotelHeat() {
        //初始化任务日志
        SparkTask taskLog = new SparkTask();
        taskLog.setTaskName("定时任务计算Top10城市的热度最高的10家酒店 : " + LocalDateTime.now());
        taskLog.setTaskType(SparkTask.USER_PROFILING_TASK); // 建议增加专门的常量
        taskLog.setStartTime(new Date());
        taskLog.setStatus(SparkTask.TASK_RUNNING);
        sparkTaskMapper.insert(taskLog);

        SparkSession spark = null;
        try {
            // 1. 初始化 Spark
            SparkConf conf = new SparkConf()
                    .setAppName("Top10CityRecommendHotel-" + System.currentTimeMillis())
                    .setMaster("local[1]")
                    .set("spark.sql.adaptive.enabled", "true")
                    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                    .set("spark.executor.extraJavaOptions", "-Xss50m -XX:+UseG1GC")
                    .set("spark.driver.extraJavaOptions", "-Xss50m")
                    .set("spark.sql.shuffle.partitions", "4");

            spark = SparkSession.builder().config(conf).getOrCreate();
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("driver", "com.mysql.cj.jdbc.Driver");

            // 2. 读取原始行为数据（近30天）
            String rawLogSql = "(SELECT hotel_id, city_id, action_weight FROM user_action_log " +
                    "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)) as raw_logs";
            Dataset<Row> rawLogs = spark.read().jdbc(url, rawLogSql, props);
            taskLog.setProcessedRecords((int) rawLogs.count());

            // 3. 计算“最热门的10个城市”
            //按 city_id 分组，求 action_weight 总和，排名前 10
            List<String> top10CityIds = rawLogs.groupBy("city_id")
                    .agg(sum("action_weight").as("city_total_weight"))
                    .orderBy(col("city_total_weight").desc())
                    .limit(10)
                    .select("city_id")
                    .collectAsList()
                    .stream()
                    .map(row -> String.valueOf(row.get(0)))
                    .collect(Collectors.toList());

            if (top10CityIds.isEmpty()) return;

            // 4. 仅计算这 10 个城市内部的酒店排名
            // 过滤出 top10CityIds 的数据 -> 按酒店分组 -> 窗口函数取前 10
            Dataset<Row> topHotelsDf = rawLogs
                    .filter(col("city_id").isin(top10CityIds.toArray())) //只取前十城市
                    .groupBy("city_id", "hotel_id")
                    .agg(sum("action_weight").as("heat_score"))
                    .withColumn("rn", row_number().over(
                            Window.partitionBy("city_id").orderBy(col("heat_score").desc())
                    ))
                    .filter("rn <= 10");

            // 5. 聚合为 List 并写入 Redis
            List<Row> resultList = topHotelsDf.groupBy("city_id")
                    .agg(collect_list("hotel_id").as("hotelIds"))
                    .collectAsList();

            Map<String,List<String>> redisResult = new HashMap<>();
            for (Row row : resultList) {
                String cityId = String.valueOf(row.get(0));
                List<String> hotelIds = row.getList(1);
                redisResult.put(cityId,hotelIds);
            }

            //存入redis
            redisTemplate.opsForValue().set("homi:hotel:city:recommend",JSON.toJSONString(redisResult));
            System.out.println(">>> 热门城市榜单更新完成，处理城市数：" + resultList.size());

        } catch (Throwable t) {
            t.printStackTrace();
            taskLog.setStatus(SparkTask.TASK_ERROR);
            String errorMsg = t.getClass().getSimpleName() + ": " + t.getMessage();
            taskLog.setErrorMsg(errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
        } finally {
            if (spark != null) {
                spark.stop();
            }
            taskLog.setEndTime(new Date());
            if (taskLog.getStartTime() != null) {
                long seconds = (taskLog.getEndTime().getTime() - taskLog.getStartTime().getTime()) / 1000;
                taskLog.setDuration(seconds);
            }
            sparkTaskMapper.updateById(taskLog);
        }

    }

    /**
     * 定时每天凌晨两点计算用户画像
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void processUserProfile() {
        // 1. 初始化任务日志
        SparkTask taskLog = new SparkTask();
        taskLog.setTaskName("定时任务计算用户画像 : " + LocalDateTime.now());
        taskLog.setTaskType(SparkTask.USER_PROFILING_TASK); // 建议增加专门的常量
        taskLog.setStartTime(new Date());
        taskLog.setStatus(SparkTask.TASK_RUNNING);
        sparkTaskMapper.insert(taskLog);

        SparkSession spark = null;
        try {
            // 2. Spark 配置 (保持你的优化参数)
            SparkConf conf = new SparkConf()
                    .setAppName("UserProfileTask-" + System.currentTimeMillis())
                    .setMaster("local[1]")
                    .set("spark.sql.adaptive.enabled", "true")
                    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                    .set("spark.executor.extraJavaOptions", "-Xss50m -XX:+UseG1GC")
                    .set("spark.driver.extraJavaOptions", "-Xss50m")
                    .set("spark.sql.shuffle.partitions", "4");

            spark = SparkSession.builder().config(conf).getOrCreate();

            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("driver", "com.mysql.cj.jdbc.Driver");
            props.setProperty("fetchSize", "1000");

            // 3. 多维度数据读取 (包含你实体类中新增的评分和设施字段)
            String recentDataSql = "(SELECT user_id, star, show_price, comment_score, " +
                    "hygiene_score, device_score, service_score, hotel_facilities " +
                    "FROM user_action_log " +
                    "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 90 DAY)) as recent_actions";

            Dataset<Row> actionLogs = spark.read().jdbc(url, recentDataSql, props);

            // 4. 计算数值维度：平均星级、价格、各项评分均值
            Dataset<Row> numericProfile = actionLogs.groupBy("user_id")
                    .agg(
                            avg("star").as("avgStar"),
                            avg("show_price").as("avgPrice"),
                            avg("comment_score").as("avgScore"),
                            avg("hygiene_score").as("avgHygiene"),
                            avg("device_score").as("avgDevice"),
                            avg("service_score").as("avgService")
                    );

            // 5. 计算标签维度：提取出现频率最高的前3个酒店设施
            Dataset<Row> facilityProfile = actionLogs
                    .withColumn("f_array", split(col("hotel_facilities"), ","))
                    .withColumn("single_f", explode(col("f_array")))
                    .filter("single_f != ''")
                    .groupBy("user_id", "single_f")
                    .count()
                    .withColumn("rn", row_number().over(
                            org.apache.spark.sql.expressions.Window.partitionBy("user_id").orderBy(col("count").desc())
                    ))
                    .filter("rn <= 3")
                    .groupBy("user_id")
                    .agg(collect_list("single_f").as("topFacilities"));

            // 6. 关联数值与标签
            Dataset<Row> finalProfileDf = numericProfile.alias("n")
                    .join(facilityProfile.alias("f"),
                            functions.col("n.user_id").equalTo(functions.col("f.user_id")),
                            "left")
                    .drop(functions.col("f.user_id")); // 删掉多余的一列

            // 7. 拉回 Driver 端写入 Redis (防止序列化异常)
            List<Row> profileList = finalProfileDf.collectAsList();
            Map<String, String> batchMap = new HashMap<>();

            for (Row row : profileList) {
                String userId = String.valueOf(row.get(0));
                Map<String, Object> tags = new HashMap<>();

                tags.put("userId", userId);
                // 四舍五入处理星级
                tags.put("prefStar", row.get(1) != null ? Math.round(row.getDouble(1)) : 0);
                tags.put("prefPrice", row.get(2) != null ? row.getDouble(2) : 0.0);
                tags.put("prefScore", row.get(3) != null ? row.getDouble(3) : 0.0);

                // 品质偏好（针对你新增的评分字段）
                tags.put("hygienePref", row.get(4) != null ? row.getDouble(4) : 0.0);
                tags.put("devicePref", row.get(5) != null ? row.getDouble(5) : 0.0);
                tags.put("servicePref", row.get(6) != null ? row.getDouble(6) : 0.0);

                // 设施标签列表
                tags.put("facilityTags", row.get(7) != null ? row.getList(7) : new ArrayList<>());

                tags.put("updateTime", System.currentTimeMillis());

                batchMap.put(userId, JSON.toJSONString(tags));

                if (batchMap.size() >= 200) {
                    redisTemplate.opsForHash().putAll("homi:user:profile", batchMap);
                    batchMap.clear();
                }
            }
            if (!batchMap.isEmpty()) {
                redisTemplate.opsForHash().putAll("homi:user:profile", batchMap);
            }

            taskLog.setStatus(SparkTask.TASK_SUCCESS);
            System.out.println(">>> 用户画像计算完成并存入 Redis");

        } catch (Throwable t) {
            t.printStackTrace();
            taskLog.setStatus(SparkTask.TASK_ERROR);
            String errorMsg = t.getClass().getSimpleName() + ": " + t.getMessage();
            taskLog.setErrorMsg(errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
        } finally {
            if (spark != null) spark.stop();
            taskLog.setEndTime(new Date());
            if (taskLog.getStartTime() != null) {
                long seconds = (taskLog.getEndTime().getTime() - taskLog.getStartTime().getTime()) / 1000;
                taskLog.setDuration(seconds);
            }
            sparkTaskMapper.updateById(taskLog);
        }
    }

    //定时每天凌晨两点，训练基于als算法的推荐模型
    @Scheduled(cron = "0 0 2 * * ?")
    public void trainRecommendAlsTask() {
        // 1. 初始化日志
        SparkTask taskLog = new SparkTask();
        taskLog.setTaskName("定时任务ALS酒店推荐模型训练 : " + LocalDateTime.now());
        taskLog.setTaskType(SparkTask.MODEL_TRAINING_TASK);
        taskLog.setStartTime(new Date());
        taskLog.setStatus(SparkTask.TASK_RUNNING);
        sparkTaskMapper.insert(taskLog);

        try {
            doRecommendAlsTrain(taskLog);
            taskLog.setStatus(SparkTask.TASK_SUCCESS);
        } catch (Throwable t) {
            t.printStackTrace();
            taskLog.setStatus(SparkTask.TASK_ERROR);
            String errorMsg = t.getClass().getSimpleName() + ": " + t.getMessage();
            taskLog.setErrorMsg(errorMsg.length() > 500 ? errorMsg.substring(0, 500) : errorMsg);
        } finally {
            taskLog.setEndTime(new Date());
            if (taskLog.getStartTime() != null) {
                long seconds = (taskLog.getEndTime().getTime() - taskLog.getStartTime().getTime()) / 1000;
                taskLog.setDuration(seconds);
            }
            sparkTaskMapper.updateById(taskLog);
        }
    }


    private void doRecommendAlsTrain(SparkTask taskLog) throws Exception {
        SparkSession spark = null;

        try {
            SparkConf conf = new SparkConf()
                    .setAppName("Task-" + System.currentTimeMillis())
                    .setMaster("local[1]")
                    .set("spark.sql.adaptive.enabled", "true")
                    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                    .set("spark.kryoserializer.buffer.max", "512m")
                    // 内存配置
                    .set("spark.executor.memory", "4g")
                    .set("spark.driver.memory", "2g")
                    .set("spark.memory.fraction", "0.8")
                    .set("spark.memory.storageFraction", "0.3")
                    .set("spark.default.parallelism", "4")
                    // 关键：大幅增加栈大小
                    .set("spark.executor.extraJavaOptions", "-Xss50m -XX:+UseG1GC")
                    .set("spark.driver.extraJavaOptions", "-Xss50m")
                    // 降低ALS内部并行度
                    .set("spark.sql.shuffle.partitions", "4");

            spark = SparkSession.builder().config(conf).getOrCreate();

            // 设置checkpoint目录
            String checkpointDir = "D:\\ideaProjects\\hom\\temp\\spark-checkpoint\\" + taskLog.getId();
            File checkpointFile = new File(checkpointDir);
            if (!checkpointFile.exists()) {
                checkpointFile.mkdirs();
            }
            spark.sparkContext().setCheckpointDir(checkpointDir);

            // 读取数据
            Properties properties = new Properties();
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("driver", "com.mysql.cj.jdbc.Driver");
            properties.setProperty("fetchSize", "1000");

            Dataset<Row> rawData = spark.read().jdbc(url,
                    "(SELECT user_id, hotel_id, action_weight FROM user_action_log WHERE action_weight > 0) as ratings",
                    properties);

            long count = rawData.count();
            taskLog.setProcessedRecords((int) count);
            System.out.println(">>> 读取数据完成，共 " + count + " 条记录");

            // 数据采样（如果数据量太大）
            // rawData = rawData.sample(0.1);  // 采样10%的数据进行训练

            // 过滤用户行为少于5次的用户
            Dataset<Row> userCounts = rawData.groupBy("user_id").count().filter("count >= 5");
            // 过滤用户行为少于3次的酒店
            Dataset<Row> hotelCounts = rawData.groupBy("hotel_id").count().filter("count >= 3");
            Dataset<Row> filteredData = rawData.join(userCounts, "user_id").join(hotelCounts, "hotel_id");

            long filteredCount = filteredData.count();
            System.out.println(">>> 过滤后数据: " + filteredCount + " 条记录");

            // StringIndexer转换
            StringIndexerModel userModel = new StringIndexer()
                    .setInputCol("user_id")
                    .setOutputCol("user_index")
                    .setHandleInvalid("skip")
                    .fit(filteredData);

            StringIndexerModel hotelModel = new StringIndexer()
                    .setInputCol("hotel_id")
                    .setOutputCol("hotel_index")
                    .setHandleInvalid("skip")
                    .fit(filteredData);

            Dataset<Row> indexedData = hotelModel.transform(userModel.transform(filteredData))
                    .persist(StorageLevel.MEMORY_AND_DISK_SER());

            long indexedCount = indexedData.count();
            System.out.println(">>> 索引转换完成，有效数据: " + indexedCount + " 条记录");

            // 关键优化：降低ALS参数
            ALS als = new ALS()
                    .setUserCol("user_index")
                    .setItemCol("hotel_index")
                    .setRatingCol("action_weight")
                    .setImplicitPrefs(true)
                    .setRank(10)           // 降低rank
                    .setMaxIter(15)       // 减少迭代次数
                    .setRegParam(0.01)
                    .setAlpha(40.0)
                    .setCheckpointInterval(2)  // 更频繁的checkpoint
                    .setSeed(42L);

            // 训练模型
            System.out.println(">>> 开始训练ALS模型...");
            ALSModel model = als.fit(indexedData);
            System.out.println(">>> ALS 模型训练完成");

            // ========== 生成推荐 ==========
            System.out.println(">>> 开始生成推荐...");

            // 方案：展平推荐结果，避免深层嵌套
            Dataset<Row> flattenedRecs = model.recommendForAllUsers(10)
                    .select(
                            col("user_index"),
                            explode(col("recommendations")).as("recommendation")
                    )
                    .select(
                            col("user_index"),
                            col("recommendation.hotel_index").as("hotel_index"),
                            col("recommendation.rating").as("score")
                    )
                    .repartition(4); // 重分区，避免数据倾斜

            System.out.println(">>> 推荐生成完成，开始处理结果...");

            // 准备ID映射
            Map<Integer, String> userMap = new HashMap<>();
            String[] userLabels = userModel.labels();
            for (int i = 0; i < userLabels.length; i++) {
                userMap.put(i, userLabels[i]);
            }

            Map<Integer, String> hotelMap = new HashMap<>();
            String[] hotelLabels = hotelModel.labels();
            for (int i = 0; i < hotelLabels.length; i++) {
                hotelMap.put(i, hotelLabels[i]);
            }

            System.out.println(">>> ID映射准备完成，用户数: " + userMap.size() + ", 酒店数: " + hotelMap.size());

            // 广播ID映射
            Broadcast<Map<Integer, String>> userBroadcast = spark.sparkContext().broadcast(
                    userMap, scala.reflect.ClassTag$.MODULE$.apply(Map.class));
            Broadcast<Map<Integer, String>> hotelBroadcast = spark.sparkContext().broadcast(
                    hotelMap, scala.reflect.ClassTag$.MODULE$.apply(Map.class));

            // 广播Redis配置
            Map<String, Object> redisConfig = new HashMap<>();
            redisConfig.put("host", redisHost);
            redisConfig.put("port", redisPort);
            redisConfig.put("password", redisPassword);

            Broadcast<Map<String, Object>> redisConfigBroadcast = spark.sparkContext().broadcast(
                    redisConfig, scala.reflect.ClassTag$.MODULE$.apply(Map.class));

            // 处理结果并写入Redis
            flattenedRecs.foreachPartition(partition -> {
                Map<Integer, String> localUserMap = userBroadcast.getValue();
                Map<Integer, String> localHotelMap = hotelBroadcast.getValue();
                Map<String, Object> localRedisConfig = redisConfigBroadcast.getValue();

                String host = (String) localRedisConfig.get("host");
                int port = (Integer) localRedisConfig.get("port");
                String password = (String) localRedisConfig.get("password");

                // 使用Map收集每个用户的推荐列表
                Map<String, List<Map<String, Object>>> userRecsMap = new HashMap<>();

                while (partition.hasNext()) {
                    Row row = partition.next();
                    int userIndex = row.getInt(0);
                    int hotelIndex = row.getInt(1);
                    float score = row.getFloat(2);

                    String userId = localUserMap.get(userIndex);
                    String hotelId = localHotelMap.get(hotelIndex);

                    if (userId != null && hotelId != null && !hotelId.isEmpty()) {
                        Map<String, Object> recItem = new HashMap<>();
                        recItem.put("hotelId", hotelId);
                        recItem.put("score", score);

                        userRecsMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(recItem);
                    }
                }

                // 批量写入Redis
                if (!userRecsMap.isEmpty()) {
                    try (Jedis jedis = new Jedis(host, port)) {
                        if (password != null && !password.isEmpty()) {
                            jedis.auth(password);
                        }

                        Map<String, String> redisHashMap = new HashMap<>();
                        for (Map.Entry<String, List<Map<String, Object>>> entry : userRecsMap.entrySet()) {
                            // 按分数排序并取前10
                            List<Map<String, Object>> top10 = entry.getValue().stream()
                                    .sorted((a, b) -> {
                                        float scoreA = ((Number) a.get("score")).floatValue();
                                        float scoreB = ((Number) b.get("score")).floatValue();
                                        return Float.compare(scoreB, scoreA);
                                    })
                                    .limit(10)
                                    .collect(Collectors.toList());

                            redisHashMap.put(entry.getKey(), JSON.toJSONString(top10));
                        }

                        // 使用Pipeline批量写入，提高性能
                        Pipeline pipeline = jedis.pipelined();
                        for (Map.Entry<String, String> entry : redisHashMap.entrySet()) {
                            pipeline.hset(RedisConstants.HOTEL.RECOMMEND_HOTEL_HASH_KEY, entry.getKey(), entry.getValue());
                        }
                        pipeline.sync();

                        System.out.println(">>> 批次写入Redis成功，共 " + redisHashMap.size() + " 条记录");
                    } catch (Exception e) {
                        System.err.println("Redis写入失败: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Redis写入失败", e);
                    }
                }
            });

            System.out.println(">>> ALS 推荐模型训练成功并更新至 Redis");

            // 清理缓存
            indexedData.unpersist();

        } finally {
            if (spark != null) {
                spark.stop();
            }
        }
    }

}