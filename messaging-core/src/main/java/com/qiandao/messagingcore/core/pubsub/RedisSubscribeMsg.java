package com.qiandao.messagingcore.core.pubsub;

import com.alibaba.fastjson2.JSONObject;
import com.qiandao.messagingcore.core.vo.MessageBody;
import com.qiandao.messagingcore.core.service.WebSocketServerInterface;
import com.qiandao.messagingcore.core.service.impl.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息监听对象，接收订阅消息
 */
@Component
@Slf4j
public class RedisSubscribeMsg {

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private WebSocketServerInterface webSocketServerInterface;

    /**
     * 处理接收到的订阅消息
     */
    public void singleMessage(String msg) {
        log.info("single收到消息：{}", msg);
        MessageBody messageBody = JSONObject.parseObject(msg, MessageBody.class);
        String fromName = messageBody.getFromName();
        String toName = messageBody.getToName();
        String sendTime = messageBody.getSendTime();
        String content = messageBody.getContent();
        webSocketServerInterface.sendMessageByUserSub(fromName, toName, sendTime, content, msg);
    }

    /**
     * 处理接收到的订阅消息
     */
    public void roomMessage(String msg) {
        log.info("room收到消息：{}", msg);
    }

    /**
     * 处理接收到的订阅消息
     */
    public void allUserMessage(String msg) {
        log.info("allUser收到消息：{}", msg);
        webSocketServer.sendAllMessageSub(msg);
    }

}
