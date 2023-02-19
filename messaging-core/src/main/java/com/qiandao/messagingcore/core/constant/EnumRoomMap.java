package com.qiandao.messagingcore.core.constant;

import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间列表 roomList(房主id,map(成员id,成员session))
 */
public enum EnumRoomMap {

    ConcurrentHashMap;
    public ConcurrentHashMap<String, ConcurrentHashMap<String, Session>> roomList;
    {
        roomList = new ConcurrentHashMap<>();
    }
}
