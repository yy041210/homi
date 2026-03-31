package com.yy.homi.hotel.task;

import com.alibaba.fastjson.JSON;
import com.yy.homi.hotel.domain.entity.SparkTask;
import com.yy.homi.hotel.mapper.SparkTaskMapper;
import org.apache.spark.SparkConf;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.StringIndexerModel;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.*;
import org.apache.spark.storage.StorageLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;


import java.io.File;
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

    //定时每天凌晨两点，根据用户近三个月的行为日志计算用户画像
    @Scheduled(cron = "0 0 2 * * ?")
    public void processUserProfile() {
        // 1. 初始化日志
        SparkTask taskLog = new SparkTask();
        Date now = new Date();
        taskLog.setTaskName("定时任务计算用户画像 : " + now.getYear() + "-" + now.getMonth() + "-" + now.getDay());
        taskLog.setTaskType(SparkTask.MODEL_TRAINING_TASK);
        taskLog.setStartTime(new Date());
        taskLog.setStatus(SparkTask.TASK_RUNNING);
        sparkTaskMapper.insert(taskLog);

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

            //构建sparkSession
            SparkSession spark = SparkSession.builder().config(conf).getOrCreate();

            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("driver", "com.mysql.cj.jdbc.Driver");
            props.setProperty("fetchSize", "1000");
            // 使用子查询实现谓词下推，仅拉取近180天数据
            String recentDataSql = "(SELECT user_id, star, show_price, comment_score " +
                    "FROM user_action_log " +
                    "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 180 DAY)) as recent_actions";

            Dataset<Row> actionLogs = spark.read().jdbc(url, recentDataSql, props);

            // 分组统计指标：平均星级、平均价格、平均评分
            Dataset<Row> profileDf = actionLogs.groupBy("user_id")
                    .agg(
                            functions.avg("star").as("avgStar"),
                            functions.avg("show_price").as("avgPrice"),
                            functions.avg("comment_score").as("avgCommentScore")
                    );

            //将分布式数据拉回到 Driver 端内存
            List<Row> profileList = profileDf.collectAsList();

            //Driver 端使用自动注入的 redisTemplate
            Map<String, String> batchMap = new HashMap<>();
            for (Row row : profileList) {
                String userId = row.getString(0);
                Map<String, Object> tags = new HashMap<>();
                tags.put("userId",row.getString(0));
                tags.put("prefStar", Math.round(row.getDouble(1))); //四舍五入
                tags.put("prePrice",row.getDouble(2));
                tags.put("preCommentScore",row.getFloat(3));

                batchMap.put(userId, JSON.toJSONString(tags));

                if (batchMap.size() >= 200) {
                    redisTemplate.opsForHash().putAll("homi:user:profile", batchMap);
                    batchMap.clear();
                }
            }
            if (!batchMap.isEmpty()) {
                redisTemplate.opsForHash().putAll("homi:user:profile", batchMap);
            }

            System.out.println(">>> 用户画像计算完成并存入 Redis");
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

    //定时每天凌晨两点，训练基于als算法的推荐模型
    @Scheduled(cron = "0 0 2 * * ?")
    public void trainRecommendAlsTask() {
        // 1. 初始化日志
        SparkTask taskLog = new SparkTask();
        Date now = new Date();
        taskLog.setTaskName("定时任务ALS酒店推荐模型训练 : " + now.getYear() + "-" + now.getMonth() + "-" + now.getDay());
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
                            pipeline.hset("homi:hotel:recommend", entry.getKey(), entry.getValue());
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