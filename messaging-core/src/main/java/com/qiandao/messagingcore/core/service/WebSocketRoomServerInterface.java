package com.qiandao.messagingcore.core.service;

public interface WebSocketRoomServerInterface {

    /**
     * 发送消息到指定房间
     * @param fromId 当前用户
     * @param roomId 房间id
     * @param sendTime 发送时间
     * @param content 发送文本
     * @param message 发送消息 对象 json
     */
    void sendMessageByUser(String fromId, String roomId, String sendTime, String content, String message);

    /**
     * 保存聊天记录
     * @param fromId 当前用户
     * @param roomId 房间id
     * @param sendTime 发送时间
     * @param content 发送文本
     */
    void recordMsg(String fromId, String roomId, String sendTime, String content);

}
