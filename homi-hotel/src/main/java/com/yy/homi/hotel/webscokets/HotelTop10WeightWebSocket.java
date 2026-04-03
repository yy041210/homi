package com.yy.homi.hotel.webscokets;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/hotel/top10weight")
@Component
public class HotelTop10WeightWebSocket {
    // 存放所有的客户端连接
    private static CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    //广播
    public static void broadcast(Object message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(JSON.toJSONString(message));  //异步发送消息
                    System.out.println("消息已发送到 session: " + session.getId());
                } catch (Exception e) {
                    System.err.println("发送消息到 session " + session.getId() + " 失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}