package com.yy.homi.hotel.config;

import com.yy.homi.common.constant.RabbitMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange userActionLogExchange(){
        return new DirectExchange(RabbitMqConstants.USER_ACTION_LOG_EXCHANGE,true,false);
    }

    @Bean
    public Queue userActionLogQueue(){
        return new Queue(RabbitMqConstants.USER_ACTION_LOG_QUEUE,true,false,false);
    }

    @Bean
    public Binding userActionLogBinding(Queue userActionLogQueue, DirectExchange userActionLogExchange){
        return BindingBuilder.bind(userActionLogQueue).to(userActionLogExchange).with(RabbitMqConstants.USER_ACTION_LOG_ROUTING_KEY);
    }


}
