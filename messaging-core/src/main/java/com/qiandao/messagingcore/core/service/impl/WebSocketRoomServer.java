package com.qiandao.messagingcore.core.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.qiandao.messagingcommon.simpleUtils.SimpleDate;
import com.qiandao.messagingcommon.utils.RRException;
import com.qiandao.messagingcore.constant.CoreConstant;
import com.qiandao.messagingcore.core.constant.EnumRoomMap;
import com.qiandao.messagingcore.core.constant.RedisSocket;
import com.qiandao.messagingcore.core.api.WebSocketController;
import com.qiandao.messagingcore.core.vo.MessageRoomBody;
import com.qiandao.messagingcore.core.service.WebSocketRoomServerInterface;
import com.qiandao.messagingcore.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

//由于webSocket无法在网络中传输，无法通过发布订阅模式转发Session，在用户加入房间问题上陷入难题。。 2022-10-29 上午 晴

@ServerEndpoint("/core/roomsocket/{param}/{title}/{max}")
@Component
@Slf4j
public class WebSocketRoomServer implements WebSocketRoomServerInterface {

    //spring默认是单例模式，StringRedisTemplate是多对象模式，无法自动注入
    private StringRedisTemplate stringRedisTemplate = SpringUtils.getBean(StringRedisTemplate.class);

    // 与某个客户端的连接会话,存入webSocketSet的对象
    private Session session;

    private WebSocketController webSocketController = SpringUtils.getBean(WebSocketController.class);

    /**
     * 创建或加入房间
     *
     * @param session
     * @param param   用户id_房间id(即房主id)_房间属主id(等于用户id)_房间密码(为空则没有密码)  房间管理员id(id1-id2)(最多两个管理员,默认为0,后续由房主更改)
     *                创建房间举例：001_520_001[_9642]
     *                加入房间举例：002_520[_9642]
     * @param redis   room:房间id map(房间成员id,在线状态0、1)
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "param") String param, @PathParam(value = "title") String title, @PathParam(value = "max") String max) {
        // final val colonyId = webSocketController.getColonyId();          //当前服务id
        // final List<String> colonyTake = webSocketController.getColonyTake();    //要接管的服务id
        // final String serverPort = webSocketController.getServerPort();

        String[] s = param.split("_");
        String roomRedis = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_ROOM_SOCKET + ":" + s[1]);
        ConcurrentHashMap<String, Session> room = EnumRoomMap.ConcurrentHashMap.roomList.get(s[1]);
        if (StringUtils.isEmpty(roomRedis)) {
            if (room == null) {
                //创建房间
                System.out.println("创建房间");
                ConcurrentHashMap<String, Session> map = new ConcurrentHashMap<>();
                map.put(s[0], session);
                EnumRoomMap.ConcurrentHashMap.roomList.put(s[1], map);
                HashMap<String, String> redisMap = new HashMap<>();
                redisMap.put(RedisSocket.REDIS_ROOM_OWNER, s[2]);
                redisMap.put(RedisSocket.REDIS_ROOM_ADMINISTRATORS_1, "0");
                redisMap.put(RedisSocket.REDIS_ROOM_ADMINISTRATORS_2, "0");
                if (s.length == 4) {
                    //参数中传递了密码
                    redisMap.put(RedisSocket.REDIS_ROOM_PASSWORD, s[3]);
                } else {
                    redisMap.put(RedisSocket.REDIS_ROOM_PASSWORD, "0");
                }
                redisMap.put(RedisSocket.REDIS_ROOM_TITLE, title);
                redisMap.put(s[0], "1");
                redisMap.put(RedisSocket.REDIS_ROOM_MAX, max);
                redisMap.put(RedisSocket.REDIS_ROOM_LIKE, "0");
                redisMap.put(RedisSocket.REDIS_ROOM_OTHER_1, "0");
                redisMap.put(RedisSocket.REDIS_ROOM_OTHER_2, "0");
                redisMap.put(RedisSocket.REDIS_ROOM_IMAGE_COVER, "https://qiandao-blog.oss-cn-hangzhou.aliyuncs.com/202208/wHjwwP.png");
                redisMap.put(RedisSocket.REDIS_ROOM_IMAGE_BACKGROUND, "0");
                String redisRoom = JSON.toJSONString(redisMap);
                stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_ROOM_SOCKET + ":" + s[1], redisRoom);
                System.out.println("本地房间列表：" + EnumRoomMap.ConcurrentHashMap.roomList);
                //向所有人通知xxx创建了几号房
                String text = s[0] + "创建了" + s[1] + "号房，快去围观～～";
                webSocketController.sendAll(text);
            } else {
                throw new RRException("类房间列表与redis房间列表消息不同步_redis不存在当前实例存在");
            }
        } else {
            if (room != null) {
                //加入房间
                System.out.println("加入房间");
                Map roomMap = JSON.parseObject(roomRedis);
                String userHistory = (String) roomMap.get(s[0]); //获取房间里的当前请求用户
                if (StringUtils.isEmpty(userHistory)) {
                    //用户第一次加入房间
                    System.out.println("第一次加入房间,需要验证密码");
                    //校验过程
                    inspectJoin(session, s, roomRedis, room, roomMap);
                } else {
                    //用户以前加入过此房间
                    System.out.println("以前加入过此房间,不需要再次验证密码");
                    oldRoomJoin(session, s, roomRedis, room);
                    //通知房间内所有人，xxx进入房间
                    RoomStatusSend(RedisSocket.REDIS_ROOM_ROOT, s[1], s[0] + "加入房间", s[0]);
                }
            } else {
                log.info("当前实例不存在此房间,系统可能重启过,校验通过会根据远程状态迁徙房间");
                //优先校验
                Map roomMap = JSON.parseObject(roomRedis);
                String userHistory = (String) roomMap.get(s[0]); //获取房间里的当前请求用户
                if (StringUtils.isEmpty(userHistory)) {
                    //用户第一次加入房间
                    System.out.println("第一次加入房间,需要验证密码");
                    //校验过程
                    String passwd = (String) roomMap.get(RedisSocket.REDIS_ROOM_PASSWORD);
                    if (!passwd.equals("0")) {
                        //房间设置有密码，需要验证
                        if (s[2].equals(passwd)) {
                            log.info("密码验证成功");
                            newRoomJoin(session, s, roomRedis);
                            //通知房间内所有人，xxx进入房间
                            RoomStatusSend(RedisSocket.REDIS_ROOM_ROOT, s[1], s[0] + "加入房间");
                        } else {
                            log.error("密码验证失败");
                        }
                    } else {
                        log.info("该房间没有密码");
                        newRoomJoin(session, s, roomRedis);
                        //通知房间内所有人，xxx进入房间
                        RoomStatusSend(RedisSocket.REDIS_ROOM_ROOT, s[1], s[0] + "加入房间");
                    }
                } else {
                    //用户以前加入过此房间
                    System.out.println("以前加入过此房间,不需要再次验证密码");
                    //重新添加本地房间
                    newRoomJoin(session, s, roomRedis);
                    //通知房间内所有人，xxx进入房间
                    RoomStatusSend(RedisSocket.REDIS_ROOM_ROOT, s[1], s[0] + "加入房间");
                }
            }
        }
    }

    private void RoomStatusSend(String fromId, String roomId, String msg) {
        RoomStatusSend(fromId,roomId,msg,null);
    }

    /**
     * 通知房间内所有人
     *
     * @param fromId 通知者
     * @param roomId 房间号
     * @param msg    通知消息
     */
    private void RoomStatusSend(String fromId, String roomId, String msg, String userId) {
        System.out.println(fromId+" 发起通知。。");
        MessageRoomBody roomBody = new MessageRoomBody();
        roomBody.setFromName(fromId);
        roomBody.setRoomId(roomId);
        String nowtimeSSS = SimpleDate.nowtimeSSS();
        roomBody.setSendTime(nowtimeSSS);
        if (userId != null) {
            String taboo = stringRedisTemplate.opsForValue().get(CoreConstant.REDIS_ROOM_TABOO + roomId + "_" + userId);
            if (taboo != null) {
                roomBody.setContent(msg + "【禁言中】");
            } else {
                roomBody.setContent(msg);
            }
        }else {
            roomBody.setContent(msg);
        }
        String message = JSON.toJSONString(roomBody);
        sendMessageByUser(fromId, roomId, nowtimeSSS, msg, message);
    }

    private void inspectJoin(Session session, String[] s, String roomRedis, ConcurrentHashMap<String, Session> room, Map roomMap) {
        String passwd = (String) roomMap.get(RedisSocket.REDIS_ROOM_PASSWORD);
        if (!passwd.equals("0")) {
            //房间设置有密码，需要验证
            if (s[2].equals(passwd)) {
                log.info("密码验证成功");
                oldRoomJoin(session, s, roomRedis, room);
                //通知房间内所有人，xxx进入房间
                RoomStatusSend(RedisSocket.REDIS_ROOM_ROOT, s[1], s[0] + "加入房间");
            } else {
                log.error("密码验证失败");
            }
        } else {
            log.info("该房间没有密码");
            oldRoomJoin(session, s, roomRedis, room);
            //通知房间内所有人，xxx进入房间
            RoomStatusSend(RedisSocket.REDIS_ROOM_ROOT, s[1], s[0] + "加入房间");
        }
    }

    private void newRoomJoin(Session session, String[] s, String roomRedis) {
        //重新添加本地房间
        ConcurrentHashMap<String, Session> map = new ConcurrentHashMap<>();
        oldRoomJoin(session, s, roomRedis, map);
    }

    private void oldRoomJoin(Session session, String[] s, String roomRedis, ConcurrentHashMap<String, Session> room) {
        room.put(s[0], session);
        EnumRoomMap.ConcurrentHashMap.roomList.put(s[1], room);
        Map map = JSON.parseObject(roomRedis);
        map.put(s[0], "1");
        String roomJSON = JSON.toJSONString(map);
        stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_ROOM_SOCKET + ":" + s[1], roomJSON);
    }

    /**
     * 连接关闭,当前用户退出此房间,redis状态设为0
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("断开：" + session);
        EnumRoomMap.ConcurrentHashMap.roomList.forEach((key, value) -> {
            if (value.containsValue(session)) {
                ConcurrentHashMap<String, Session> room = EnumRoomMap.ConcurrentHashMap.roomList.get(key);
                String sessionKey = null;
                for (String keyset : room.keySet()) {
                    if (room.get(keyset).equals(session)) {
                        sessionKey = keyset;
                    }
                }
                String roomJSON = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_ROOM_SOCKET + ":" + key);
                Map map = JSON.parseObject(roomJSON);
                map.put(sessionKey, "0");
                String mapJSON = JSON.toJSONString(map);
                stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_ROOM_SOCKET + ":" + key, mapJSON);
                room.remove(sessionKey);
                //通知房间内所有人，xxx离开房间
                RoomStatusSend(RedisSocket.REDIS_ROOM_ROOT, key, sessionKey + "离开房间");
            }
            System.out.println("当前本地房间：" + EnumRoomMap.ConcurrentHashMap.roomList);
        });
    }

    /**
     * 接收客户端消息
     *
     * @param message 接收的消息
     */
    @OnMessage
    public void onMessage(String message) {
        log.info("收到客户端发来的消息：{}", message);
        MessageRoomBody messageRoomBody = JSONObject.parseObject(message, MessageRoomBody.class);
        if (messageRoomBody == null) {
            log.warn("监听到的消息为空或格式不正确，message:{}", message);
            return;
        }
        String fromName = messageRoomBody.getFromName();
        String sendTime = messageRoomBody.getSendTime();
        String content = messageRoomBody.getContent();
        String roomId = messageRoomBody.getRoomId();
        sendMessageByUser(fromName, roomId, sendTime, content, message);
    }

    /**
     * 推送消息到房间内除自己外所有在线用户
     *
     * @param fromId  发送者id
     * @param roomId  房间id
     * @param message 发送的消息
     */
    @Override
    public void sendMessageByUser(String fromId, String roomId, String sendTime, String content, String message) {
        ConcurrentHashMap<String, Session> room = EnumRoomMap.ConcurrentHashMap.roomList.get(roomId);
        if (room != null) {     //从当前实例能够获取到房间
            Session sessionRoom = room.get(fromId);
            if (sessionRoom != null || fromId.equals(RedisSocket.REDIS_ROOM_ROOT)) {      //发送者是否在房间内
                String roomRedis = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_ROOM_SOCKET + ":" + roomId);
                if (!StringUtils.isEmpty(roomRedis)) {  //redis中是否能获取到当前房间信息
                    Map map = JSON.parseObject(roomRedis);
                    //判断发送者是否在禁言状态
                    String taboo = stringRedisTemplate.opsForValue().get(CoreConstant.REDIS_ROOM_TABOO + roomId + "_" + fromId);
                    if (taboo == null) {
                        //拿到当前房间的所有在线用户
                        HashSet<String> online = new HashSet<>();
                        map.forEach((key, value) -> {
                            if (value.equals("1")) {
                                online.add((String) key);
                            }
                        });
                        for (String key : room.keySet()) {
                            if (!key.equals(fromId) && online.contains(key)) {
                                System.out.println(fromId + "->发信给：" + key);
                                Session session = room.get(key);
                                try {
                                    session.getBasicRemote().sendText(message);
                                } catch (Exception e) {
                                    log.error("消息发送错误：" + e.getMessage(), e);
                                }
                            }
                        }
                        recordMsg(fromId, roomId, sendTime, content);
                    } else {
                        log.info("该用户已被禁言:{}", fromId);
                    }
                } else {
                    log.error("网络或房间出现异常状况,无法获取!");
                }
            } else {
                log.error("该用户不再房间内，用户:{}", fromId);
            }
        }
    }

    //保存聊天记录
    @Override
    public void recordMsg(String fromId, String roomId, String sendTime, String content) {
        String roomMsg = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_MSG_ROOM_RECORD + ":" + roomId);
        if (StringUtils.isEmpty(roomMsg)) {
            String record = fromId + "_" + sendTime;
            HashMap<String, String> map = new HashMap<>();
            map.put(record, content);
            String recordJSON = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_MSG_ROOM_RECORD + ":" + roomId, recordJSON);
        } else {
            Map map = JSON.parseObject(roomMsg);
            System.out.println("以前的群聊记录：" + map);
            String record = fromId + "_" + sendTime;
            map.put(record, content);
            String recordJSON = JSON.toJSONString(map);
            stringRedisTemplate.opsForValue().set(RedisSocket.REDIS_MSG_ROOM_RECORD + ":" + roomId, recordJSON);
        }
        //TODO 删除超过指定天数的消息
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





