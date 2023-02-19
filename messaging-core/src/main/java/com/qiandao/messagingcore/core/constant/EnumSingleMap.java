package com.qiandao.messagingcore.core.constant;

import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人员列表 【用户id,session状态】
 */
public enum EnumSingleMap {

    ConcurrentHashMap;
    public ConcurrentHashMap<String, Session> fromMap;
    {
        fromMap = new ConcurrentHashMap<>();
    }
}
