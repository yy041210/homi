package com.yy.homi.hotel.listeners;

import com.yy.homi.common.constant.RabbitMqConstants;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.hotel.domain.entity.HotelEsSyncMessage;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class HotelEsSyncListener {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    @RabbitListener(queues = {RabbitMqConstants.HOTEL_ES_SYNC_QUEUE})
    public void hotelEsSyncQueueListener(HotelEsSyncMessage message){
        log.info("接收到酒店 es同步消息! 消息体：{}",message);
        if(message == null){
            return;
        }

        String businessType = message.getBusinessType();
        if(businessType.equals(HotelEsSyncMessage.SYNC_ONLY_TYPE)){
            //同步一个

        } else if (businessType.equals(HotelEsSyncMessage.SYNC_BATCH_TYPE)) {
            //同步一批

        }else if (businessType.equals(HotelEsSyncMessage.DELETE_ONLY_TYPE)) {
            //删除一个
            String hotelId = message.getData().toString();

            // 创建按条件删除请求
            DeleteByQueryRequest request = new DeleteByQueryRequest("hoteldoc");

            // 设置查询条件：匹配 hotelId 字段
            request.setQuery(QueryBuilders.termQuery("hotelId", hotelId));

            try {
                restHighLevelClient.deleteByQuery(request,RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new ServiceException("es根据id删除文档异常！");
            }

        }else if (businessType.equals(HotelEsSyncMessage.DELETE_BATCH_TYPE)) {
            //删除一批

        }else{
            log.error("{} 队列接收到未知消息类型！消息体：{}",RabbitMqConstants.HOTEL_ES_SYNC_QUEUE,message);
        }

    }
}
