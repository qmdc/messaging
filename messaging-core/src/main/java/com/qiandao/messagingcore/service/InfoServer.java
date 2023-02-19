package com.qiandao.messagingcore.service;

import java.util.List;
import java.util.Map;

public interface InfoServer {

    /**
     * 获取所有在线用户ID
     * @return list
     */
    List<String> getAllOnlineUser();

    /**
     * 获取两用户之间的历史聊天记录
     * @param fromId 当前用户
     * @param toId 对方
     * @return map
     */
    Map<String, String> getUserMsg(String fromId, String toId);

    /**
     * 获取所有房间列表及详细情况
     * @return list
     */
    List<Map<String, String>> getAllRoom();

    /**
     * 根据房间id获取房间内历史消息
     * @param roomId 房间id
     * @return map
     */
    Map<String, String> getRoomMsg(String roomId);

    /**
     * 根据房间id获取房间内所有信息
     * @param roomId 房间id
     * @return list
     */
    List<Map<String, String>> getRoomInfoById(String roomId);

}
