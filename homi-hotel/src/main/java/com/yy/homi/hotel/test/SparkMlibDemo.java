package com.yy.homi.hotel.test;


import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Properties;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.explode;

public class SparkMlibDemo {
    public static void main(String[] args) {
        // 1. 初始化
        SparkSession spark = SparkSession.builder()
                .appName("HotelRecommender")
                .master("local[*]")
                .getOrCreate();

        // 2. 读取数据 (假设你已经有了 url 和 props)
        String url = "jdbc:mysql://localhost:3306/homi_hotel?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8";
        Properties props = new Properties();
        props.setProperty("user","root");
        props.setProperty("password","1234");
        Dataset<Row> rawData = spark.read().jdbc(url, "user_action_log", props);

        // 3. 【修正点】转换 String ID 为数字 Index
        // StringIndexer 必须 fit 再 transform 才会产生新列
        Dataset<Row> indexedData = new StringIndexer()
                .setInputCol("user_id").setOutputCol("userIndex")
                .fit(rawData).transform(rawData);

        indexedData = new StringIndexer()
                .setInputCol("hotel_id").setOutputCol("hotelIndex")
                .fit(indexedData).transform(indexedData);

        // 4. 【增强点】配置 ALS 算法（加入隐式反馈参数）
        // 假设这些参数是从你的“算法配置管理”数据库里读出来的
        int rank = 10;
        double alpha = 40.0; // 隐式反馈的置信度，通常设为 40

        ALS als = new ALS()
                .setUserCol("userIndex")
                .setItemCol("hotelIndex")
                .setRatingCol("action_weight")
                .setRank(rank)
                .setMaxIter(10)
                .setRegParam(0.1)
                // --- 关键新增 ---
                .setImplicitPrefs(true) // 开启隐式反馈，适合点击/收藏日志
                .setAlpha(alpha);       // 设置偏好强度

        // 5. 【修正点】训练模型需要传入数据集
        ALSModel model = als.fit(indexedData);

        // 6. 为所有用户生成前 10 个推荐
        Dataset<Row> recommendations = model.recommendForAllUsers(10);

        // 7. 【重要】将 Index 转回原始的 String ID 才能存入数据库给 Java 用
        // 推荐结果默认是 [userIndex, recommendations(hotelIndex, rating)]
        // 你需要处理这个嵌套结构，并把 hotelIndex 还原回 hotelId
        recommendations.show();

        // 或者将结果转换为更易读的格式
        recommendations.printSchema();

// 如果需要展开查看每个推荐
        Dataset<Row> expanded = recommendations
                .select(col("userIndex"), explode(col("recommendations")).as("rec"))
                .select(col("userIndex"), col("rec.hotelIndex"), col("rec.rating"));
        expanded.show(20, false);

        spark.stop();
    }
}
