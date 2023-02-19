package com.qiandao.messagingcore.service;

public interface UserTabooServer {

    /**
     * 禁言用户
     * @param roomId 房间id
     * @param userId 用户id
     * @param time 封禁时长（秒）
     * @param msg 封禁理由
     */
    boolean taboo(String roomId,String userId,String time,String msg);

}
