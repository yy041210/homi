package com.yy.homi.hotel.task;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.yy.homi.common.constant.RabbitMqConstants;
import com.yy.homi.hotel.config.RabbitMQReceiver;
import com.yy.homi.hotel.domain.entity.HotelBase;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import com.yy.homi.hotel.mapper.HotelBaseMapper;
import com.yy.homi.hotel.webscokets.HotelTop10WeightWebSocket;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;

import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SparkStreamingTask implements Serializable {

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;
    @Value("${spring.rabbitmq.port}")
    private int rabbitPort;
    @Value("${spring.rabbitmq.username}")
    private String rabbitUser;
    @Value("${spring.rabbitmq.password}")
    private String rabbitPass;
    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Autowired
    private HotelBaseMapper hotelBaseMapper;

    @Autowired
    private transient StringRedisTemplate stringRedisTemplate;

    public void startRealTimeProcessing() {
        // 1. 配置：设置独立 UI 端口 4040，分配 2 个核心
        SparkConf conf = new SparkConf()
                .setAppName("Homi-RealTime-Heat")
                .setMaster("local[2]")
                .set("spark.ui.port", "4041");

        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(3)); //批处理时间(3s从消息队列拉一次消息)

        // 2. 实例化自定义接收器
        RabbitMQReceiver receiver = new RabbitMQReceiver(
                rabbitHost,
                rabbitPort,
                rabbitUser,
                rabbitPass,
                virtualHost,
                RabbitMqConstants.USER_ACTION_LOG_QUEUE
        );

        // 3. 创建数据流
        JavaReceiverInputDStream<String> msgStream = jssc.receiverStream(receiver);

        // 4. 计算逻辑
        Map<String, Long> idWeightMap = new HashMap<>();
        msgStream.map(json -> {
                    try {
                        // 解析为你的实体类
                        UserActionLog log = JSON.parseObject(json, UserActionLog.class);
                        return log;
                    } catch (Exception e) {
                        return null; // 过滤脏数据
                    }
                })
                .filter(userActionLog -> userActionLog != null)
                .mapToPair(log -> {
                    // 使用酒店ID作为Key，权重分作为Value
                    //优先取 actionWeight，如果为空则调用实体类的获取权重方法
                    Double weight = log.getActionWeight() != null ?
                            log.getActionWeight() :
                            UserActionLog.getWeightByType(log.getActionType());
                    return new Tuple2<>(log.getHotelId(), weight.longValue());
                })
                .reduceByKeyAndWindow(Long::sum, Durations.seconds(24 * 60), Durations.seconds(3))  //24h窗口，3s(滑动窗口计算一次)
                .foreachRDD(rdd -> {
                    if (!rdd.isEmpty()) {
                        // 将 (酒店ID, 分数) 转换为 (分数, 酒店ID) 以便利用 Spark 的 sortByKey 进行排序
                        List<Tuple2<String, Long>> top10 = rdd.mapToPair(Tuple2::swap)
                                // false 表示降序排序
                                .sortByKey(false)
                                // 再转回 (酒店ID, 分数)
                                .mapToPair(Tuple2::swap)
                                // 取热度最高的前 10 个
                                .take(10);

                        //计算结果写入websocket

                        for (Tuple2<String, Long> tuple2 : top10) {
                            String hotelId = tuple2._1;
                            Long totalWeight = tuple2._2;
                            idWeightMap.put(hotelId, totalWeight);
                        }
                        System.out.println("=================================================");
                        System.out.println("实时权重计算：" + top10);
                        System.out.println("=================================================");
                    }

                    List<String> hotelIds = idWeightMap.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());

                    if(CollectionUtil.isNotEmpty(hotelIds)){
                        //处理数据发送到websocket session
                        List<HotelBase> hotelBases = hotelBaseMapper.selectBatchIds(hotelIds);
                        Map<String, HotelBase> hotelIdMap = CollStreamUtil.toIdentityMap(hotelBases, HotelBase::getId);

                        Map<String, Long> result = idWeightMap.entrySet().stream()
                                .filter(entry -> hotelIdMap.containsKey(entry.getKey()))
                                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // 按 value 降序
                                .collect(Collectors.toMap(
                                        entry -> hotelIdMap.get(entry.getKey()).getName(),
                                        Map.Entry::getValue,
                                        (e1, e2) -> e1,
                                        LinkedHashMap::new  // 保持顺序
                                ));


                        HotelTop10WeightWebSocket.broadcast(result);
                    }

                });

        jssc.start();
        try {
            System.out.println(">>> 实时任务启动，WebUI：http://localhost:4041");
            jssc.awaitTermination();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateRedis(List<Tuple2<Long, Long>> data) {
        String key = "homi:realtime:hot";
        stringRedisTemplate.delete(key);
        data.forEach(t -> stringRedisTemplate.opsForZSet().add(key, String.valueOf(t._1), t._2));
    }

    private int getWeight(String type) {
        if ("ORDER".equalsIgnoreCase(type)) return 15;
        if ("COLLECT".equalsIgnoreCase(type)) return 5;
        return 1;
    }
}