package com.qiandao.messagingcore.core.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.qiandao.messagingcore.core.constant.EnumSingleMap;
import com.qiandao.messagingcore.core.constant.RedisSocket;
import com.qiandao.messagingcore.core.vo.MessageBody;
import com.qiandao.messagingcore.core.service.WebSocketServerInterface;
import com.qiandao.messagingcore.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@ServerEndpoint("/core/websocket/{fromId}")
@Component
@Slf4j
public class WebSocketServer implements WebSocketServerInterface {

    //spring默认是单例模式，StringRedisTemplate是多对象模式，无法自动注入
    private StringRedisTemplate stringRedisTemplate = SpringUtils.getBean(StringRedisTemplate.class);

    // concurrent包的线程安全Set,用来存放每个客户端对应的WebSocket对象，仅方便统计当前机器实例的在线session数
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();

    /**
     * 建立WebSocket连接
     *
     * @param session session
     * @param fromId  用户ID
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "fromId") String fromId) {
        log.info("WebSocket建立中,from用户ID：{}", fromId);
        //在redis中保存状态
        String key = RedisSocket.REDIS_STATUS_SOCKET + ":" + fromId;
        //设置当前webSocket的状态
        stringRedisTemplate.opsForValue().set(key, "1");
        //在本地map中保存连接socket信息
        EnumSingleMap.ConcurrentHashMap.fromMap.put(key, session);
        // 与某个客户端的连接会话,存入webSocketSet的对象
        webSocketSet.add(this);
        log.info("建立连接完成,当前实例在线人数为：{}", webSocketSet.size());
        System.out.println(EnumSingleMap.ConcurrentHashMap.fromMap);
        //检查是否有自己的历史未接收消息
        historyMsg(fromId);
    }

    /**
     * 检查是否有自己的历史未接收消息
     *
     * @param fromId 当前登录用户id
     */
    private void historyMsg(String fromId) {
        Set<String> keys = stringRedisTemplate.keys(RedisSocket.REDIS_USER_NOT_ONLINE + ":" + "*" + "_" + fromId);
        if (keys != null) {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            Iterator<String> keyIter = keys.iterator();
            Iterator<String> valueIter = values.iterator();
            int len = keys.size();
            Map<String, String> map = new HashMap<>(); //封装对应的key、value到map中
            for (int i = 0; i < len; i++) {
                if (keyIter.hasNext() && valueIter.hasNext()) {
                    String keyNext = keyIter.next();
                    String valueNext = valueIter.next();
                    map.put(keyNext, valueNext);
                }
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                //String notOnline = entry.getKey();  //notOnline:1_2
                String msg = entry.getValue();
                Map messages = JSON.parseObject(msg);
                Iterator<Map.Entry<String, String>> iterator = messages.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> next = iterator.next();
                    //String key1 = next.getKey();        //2022-09-30 10:51:32
                    String values1 = next.getValue();   //{\"fromName\":\"1\",\"toName\":\"2\",\"content\":\"消息内容\"}
                    MessageBody messageBody = JSONObject.parseObject(values1, MessageBody.class);
                    String fromName = messageBody.getFromName();
                    String toName = messageBody.getToName();
                    String sendTime = messageBody.getSendTime();
                    String content = messageBody.getContent();
                    sendMessageByUserSub(fromName, toName, sendTime, content, values1);
                }
            }
            //从redis中批量移除
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("断开：" + session);
        boolean contains = EnumSingleMap.ConcurrentHashMap.fromMap.contains(session);
        if (contains) {
            String key = null;
            for (String keyset : EnumSingleMap.ConcurrentHashMap.fromMap.keySet()) {
                if (EnumSingleMap.ConcurrentHashMap.fromMap.get(keyset).equals(session)) {
                    key = keyset;
                }
            }
            EnumSingleMap.ConcurrentHashMap.fromMap.remove(key);    //移除本地map的session
            //改变redis的在线标标志状态
            String getRecord = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isEmpty(getRecord)) {
                log.error("redis用户状态与本地map存在数据不同步错误！redis中不存在:{}", key);
            } else {
                stringRedisTemplate.opsForValue().set(key, "0");   //重新将对应的标识位设置位"0",代表下线
            }
        }
        webSocketSet.remove(this);  //移除本地session
        log.info("连接断开,当前实例在线人数为：{}", webSocketSet.size());
        System.out.println(EnumSingleMap.ConcurrentHashMap.fromMap);
    }

    /**
     * 接收客户端消息
     *
     * @param message 接收的消息
     */
    @OnMessage
    public void onMessage(String message) {
        log.info("收到客户端发来的消息：{}", message);
        MessageBody messageBody = JSONObject.parseObject(message, MessageBody.class);
        if (messageBody == null) {
            log.warn("监听到的消息为空或格式不正确，message:{}", message);
            return;
        }
        String fromName = messageBody.getFromName();
        String toName = messageBody.getToName();
        String sendTime = messageBody.getSendTime();
        String content = messageBody.getContent();
        sendMessagePub(fromName, toName, sendTime, content, message);
    }

    /**
     * 集群模式下采用redis发布订阅模式推送
     */
    @Override
    public void sendMessagePub(String fromId, String toId, String sendTime, String content, String message) {
        log.info("from：" + fromId + " to:" + toId + ",推送时间：" + sendTime + ",推送内容：" + content);
        String s = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_STATUS_SOCKET + ":" + toId);//拿到toId的状态
        if (Objects.equals(s, "1")) {
            stringRedisTemplate.convertAndSend(RedisSocket.REDIS_CHANNEL_SINGLE, message);
        } else {
            //对方不在线时将消息存入redis，待对方上线时先获取redis中数据
            saveOffMsg(fromId, toId, sendTime, message);
        }
    }

    /**
     * 接收redis订阅消息
     *
     * @param toId    用户ID
     * @param content 发送的文字
     * @param message 发送的消息对象 json 对象
     */
    @Override
    public void sendMessageByUserSub(String fromId, String toId, String sendTime, String content, String message) {
        Session session = EnumSingleMap.ConcurrentHashMap.fromMap.get(RedisSocket.REDIS_STATUS_SOCKET + ":" + toId);
        if (session != null) {
            try {
                session.getBasicRemote().sendText(message);
                recordMsg(fromId, toId, sendTime, content);
            } catch (IOException e) {
                log.error("消息发送错误：" + e.getMessage(), e);
            }
        }
    }

    /**
     * 保存离线消息
     *
     * @param fromId   fromId
     * @param toId     toId
     * @param sendTime 发送时间
     * @param message  消息 json 对象
     */
    private void saveOffMsg(String fromId, String toId, String sendTime, String message) {
        String history = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_USER_NOT_ONLINE + ":" + fromId + "_" + toId);
        JSONObject map = JSON.parseObject(history);
        System.out.println("历史待发送消息:" + map);
        if (map == null) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(sendTime, message);
            String toJSONString = JSON.toJSONString(hashMap);
            stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_USER_NOT_ONLINE + ":" + fromId + "_" + toId, toJSONString);
        } else {
            map.put(sendTime, message);
            String jsonString = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_USER_NOT_ONLINE + ":" + fromId + "_" + toId, jsonString);
        }
    }

    /**
     * 保存聊天记录
     *
     * @param fromId   fromId
     * @param toId     toId
     * @param sendTime sendTime
     * @param content  content
     */
    public void recordMsg(String fromId, String toId, String sendTime, String content) {
        String s1 = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_MSG_RECORD + ":" + fromId + "_" + toId);
        String s2 = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_MSG_RECORD + ":" + toId + "_" + fromId);
        if (s1 != null) {
            Map<String, Object> map = JSON.parseObject(s1);
            System.out.println("两个人以前的聊天记录：" + map);
            String record = fromId + "_" + toId + "_" + sendTime;
            map.put(record, content);
            String msg = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_MSG_RECORD + ":" + fromId + "_" + toId, msg);
        } else if (s2 != null) {
            Map<String, Object> map = JSON.parseObject(s2);
            System.out.println("两个人以前的聊天记录：" + map);
            String record = fromId + "_" + toId + "_" + sendTime;
            map.put(record, content);
            String msg = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_MSG_RECORD + ":" + toId + "_" + fromId, msg);
        } else {
            HashMap<String, String> map = new HashMap<>();
            String record = fromId + "_" + toId + "_" + sendTime;
            map.put(record, content);
            String msg = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_MSG_RECORD + ":" + fromId + "_" + toId, msg);
        }
        //TODO 删除超过指定天数的消息
    }

    /**
     * 群发广播消息给在线用户(集群模式)
     *
     * @param message 消息 json 对象
     */
    @Override
    public void sendAllMessagePub(String message) {
        log.info("群体发送消息给所有在线用户：{}", message);
        stringRedisTemplate.convertAndSend(RedisSocket.REDIS_CHANNEL_ALL_USER, message);
    }

    public void sendAllMessageSub(String message) {
        EnumSingleMap.ConcurrentHashMap.fromMap.forEach((key, value) -> {
            try {
                value.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("群发消息给用户:{} 发生错误：{}", key, e.getMessage());
            }
        });
    }


    /**
     * @deprecated
     */
    @Override
    public void sendAllMessage(String message) {
        log.info("群体发送消息给所有在线用户：{}", message);
        Set<String> keys = stringRedisTemplate.keys(RedisSocket.REDIS_STATUS_SOCKET + "*"); //拿到以STATUS前缀开头的所有key值
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        Iterator<String> keyIter = keys.iterator();
        Iterator<String> valueIter = values.iterator();
        int len = keys.size();
        HashMap<String, String> map = new HashMap<>(); //封装对应的key、value到map中
        for (int i = 0; i < len; i++) {
            if (keyIter.hasNext() && valueIter.hasNext()) {
                String keyNext = keyIter.next();
                String valueNext = valueIter.next();
                map.put(keyNext, valueNext);
            }
        }
        List<String> collect = keys.stream().map(key -> {
            String keyV = null;
            String s = map.get(key);
            if (!StrUtil.isEmpty(s)) {
                if (s.equals("1")) {
                    keyV = key;
                }
            }
            return keyV;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        //TODO 带上每个用户名
        for (String s : collect) {
            Session session = EnumSingleMap.ConcurrentHashMap.fromMap.get(s);
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException | NullPointerException e) {
                log.error("群发消息发生错误：" + e.getMessage(), e);
            }
        }
    }

    /**
     * 发生错误
     *
     * @param throwable e
     */
    @OnError
    public void onError(Throwable throwable) {
        log.error("发消息发生错误：" + throwable);
        throwable.printStackTrace();
    }

}




