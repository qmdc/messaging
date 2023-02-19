package com.qiandao.messagingcore.core.service;

public interface WebSocketServerInterface {

     void sendMessageByUserSub(String fromId, String toId, String sendTime, String content, String message);

     void recordMsg(String fromId, String toId, String sendTime, String content);

     void sendMessagePub(String fromId, String toId, String sendTime, String content, String message);

     /** @deprecated */
     void sendAllMessage(String message);

     void sendAllMessagePub(String message);

}
