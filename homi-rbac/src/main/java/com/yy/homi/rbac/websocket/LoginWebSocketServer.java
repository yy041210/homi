package com.yy.homi.rbac.websocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//微信扫码登录响应得websocketserver
@Slf4j
@Component
@ServerEndpoint("/ws/login/{sceneId}")
public class LoginWebSocketServer {
    // 存放所有在线的 PC 端 Session
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sceneId") String sceneId) {
        SESSIONS.put(sceneId, session);
        log.info("PC端连接成功，等待扫码。场景值：" + sceneId);
    }

    @OnClose
    public void onClose(@PathParam("sceneId") String sceneId) {
        SESSIONS.remove(sceneId);
        log.info("PC端断开，场景值：" + sceneId);
    }

    @OnError
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    /**
     * 关键：供外部调用的发送方法
     */
    /**
     * 供外部调用的发送方法
     *
     * @param sceneId 场景ID（暗号）
     * @param data    要发送的对象（通常是 R 对象）
     */
    public static void sendMessage(String sceneId, Object data) {
        Session session = SESSIONS.get(sceneId);
        if (session != null && session.isOpen()) {
            try {
                // 这里将对象转为 JSON 字符串
                String message = JSON.toJSONString(data);

                // 使用同步发送文本消息
                session.getBasicRemote().sendText(message);

                log.info("成功向场景 [" + sceneId + "] 推送消息");
            } catch (IOException e) {
                log.error("推送消息失败：{}", e.getMessage(), e);
            }
        } else {
            log.error("推送失败：场景 [" + sceneId + "] 的连接已失效或不存在");
        }
    }

}
