package com.yy.homi.rbac.config;

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
    public DirectExchange homiLogExchange(){
        return new DirectExchange(RabbitMqConstants.HOMI_LOG_EXCHANGE,true,false);
    }

    @Bean
    public Queue homiLogQueue(){
        return new Queue(RabbitMqConstants.HOMI_LOG_QUEUE,true,false,false);
    }

    @Bean
    public Binding homiLogBinding(Queue homiLogQueue, DirectExchange homiLogExchange){
        return BindingBuilder.bind(homiLogQueue).to(homiLogExchange).with(RabbitMqConstants.HOMI_LOG_ROUTING_KEY);
    }


}
