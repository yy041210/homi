package com.yy.homi.hotel.config;

import com.rabbitmq.client.*;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;

import java.nio.charset.StandardCharsets;

/**
 * 自定义 RabbitMQ 接收器
 * 优点：直接使用项目已有的 amqp-client 依赖
 */
public class RabbitMQReceiver extends Receiver<String> {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String queueName;
    private final String virtualHost;

    public RabbitMQReceiver(String host, int port, String user, String password, String virtualHost, String queueName) {
        super(StorageLevel.MEMORY_ONLY());
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.queueName = queueName;
        this.virtualHost = virtualHost;
    }

    @Override
    public void onStart() {
        // 开启新线程异步接收数据，避免阻塞 Spark Receiver 管理线程
        new Thread(this::receive).start();
    }

    @Override
    public void onStop() {
        // 停止时的清理逻辑（可选）
    }

    private void receive() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(user);
            factory.setPassword(password);
            factory.setVirtualHost(virtualHost);

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // 确保队列存在（持久化、非排他、不自动删除）
            channel.queueDeclare(queueName, true, false, false, null);

            // 定义回调逻辑
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    String message = new String(body, StandardCharsets.UTF_8);
                    // 核心逻辑：将抓取到的字符串存入 Spark 引擎
                    store(message);
                }
            };

            // 启动消费，设置自动应答(AutoAck)
            channel.basicConsume(queueName, true, consumer);

        } catch (Exception e) {
            // 遇到异常通知 Spark 重启该 Receiver
            restart("RabbitMQ 连接失败，正在重试...", e);
        }
    }
}