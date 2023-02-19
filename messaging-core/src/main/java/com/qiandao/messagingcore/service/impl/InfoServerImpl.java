package com.qiandao.messagingcore.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.qiandao.messagingcore.core.constant.RedisSocket;
import com.qiandao.messagingcore.service.InfoServer;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InfoServerImpl implements InfoServer{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> getAllOnlineUser() {
        Set<String> keys = stringRedisTemplate.keys(RedisSocket.REDIS_STATUS_SOCKET + "*");
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);    //获取所有value
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
        List<String> onlineUserIds = new ArrayList<>();
        for (String key : map.keySet()) {
            if (map.get(key).equals("1")) {
                onlineUserIds.add(key.split(":")[1]);
            }
        }
        return onlineUserIds;
    }

    @Override
    public Map<String, String> getUserMsg(String fromId, String toId) {
        String userMsg = null;
        String s1 = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_MSG_RECORD + ":" + fromId + "_" + toId);
        if (!StrUtil.isEmpty(s1)) {
            userMsg = s1;
        } else {
            String s2 = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_MSG_RECORD + ":" + toId + "_" + fromId);
            if (!StrUtil.isEmpty(s2)) {
                userMsg = s2;
            }
        }
        Map map = JSON.parseObject(userMsg);
        System.out.println(map);
        return map;
    }

    @Override
    public List<Map<String, String>> getAllRoom() {
        Set<String> keys = stringRedisTemplate.keys(RedisSocket.REDIS_ROOM_SOCKET + "*");
        List<String> rooms = stringRedisTemplate.opsForValue().multiGet(keys);
        List<Map> collect = rooms.stream().map(room -> {
            Map map = JSON.parseObject(room);
            //map.remove(RedisSocket.REDIS_ROOM_OWNER);
            //map.remove(RedisSocket.REDIS_ROOM_PASSWORD);
            //map.remove(RedisSocket.REDIS_ROOM_TITLE);
            //map.remove(RedisSocket.REDIS_ROOM_MAX);
            map.remove(RedisSocket.REDIS_ROOM_ADMINISTRATORS_1);
            map.remove(RedisSocket.REDIS_ROOM_ADMINISTRATORS_2);
            return map;
        }).collect(Collectors.toList());
        System.out.println("map状态：" + collect);
        List<String> allRooms = keys.stream().map(key -> {
            val split = key.split(":");
            return split[1];
        }).collect(Collectors.toList());
        Iterator<String> roomIter = allRooms.iterator();
        Iterator<Map> infoIter = collect.iterator();
        List<Map<String, String>> maps = new ArrayList<>();
        for (int i = 0; i < allRooms.size(); i++) {
            String room = roomIter.next();
            Map<String, String> info = infoIter.next();
            Map<String, String> infoMap = new HashMap<>();
            int num = 0;
            for (String s : info.keySet()) {
                if (info.get(s) != null && info.get(s).equals("1")) {
                    num++;
                }
            }
            infoMap.put("allPeople", String.valueOf(info.size() - 4));
            infoMap.put("online", String.valueOf(num));
            infoMap.put("roomId", room);
            infoMap.put(RedisSocket.REDIS_ROOM_OWNER, info.get(RedisSocket.REDIS_ROOM_OWNER));
            infoMap.put(RedisSocket.REDIS_ROOM_TITLE, info.get(RedisSocket.REDIS_ROOM_TITLE));
            infoMap.put(RedisSocket.REDIS_ROOM_MAX, info.get(RedisSocket.REDIS_ROOM_MAX));
            infoMap.put(RedisSocket.REDIS_ROOM_LIKE, info.get(RedisSocket.REDIS_ROOM_LIKE));
            infoMap.put(RedisSocket.REDIS_ROOM_IMAGE_COVER, info.get(RedisSocket.REDIS_ROOM_IMAGE_COVER));
            infoMap.put(RedisSocket.REDIS_ROOM_IMAGE_BACKGROUND, info.get(RedisSocket.REDIS_ROOM_IMAGE_BACKGROUND));
            infoMap.put(RedisSocket.REDIS_ROOM_OTHER_1, info.get(RedisSocket.REDIS_ROOM_OTHER_1));
            infoMap.put(RedisSocket.REDIS_ROOM_OTHER_2, info.get(RedisSocket.REDIS_ROOM_OTHER_2));
            //返回的密码是经过md5加密后的密文
            String pwd = info.get(RedisSocket.REDIS_ROOM_PASSWORD);
            String password = DigestUtil.md5Hex(pwd);
            infoMap.put("password", password);
            maps.add(infoMap);
        }
        return maps;
    }

    @Override
    public Map<String, String> getRoomMsg(String roomId) {
        String record = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_MSG_ROOM_RECORD + ":" + roomId);
        if (StrUtil.isEmpty(record)) {
            return null;
        } else {
            Map map = JSON.parseObject(record);
            System.out.println(map);
            return map;
        }
    }

    @Override
    public List<Map<String, String>> getRoomInfoById(String roomId) {
        String room = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_ROOM_SOCKET + ":" + roomId);
        if (StrUtil.isEmpty(room)) {
            return null;
        } else {
            List<Map<String, String>> info = new ArrayList<>();
            Map roomMap = JSON.parseObject(room);
            String pwd = (String) roomMap.get(RedisSocket.REDIS_ROOM_PASSWORD);
            String password = DigestUtil.md5Hex(pwd);
            String owner = (String) roomMap.get(RedisSocket.REDIS_ROOM_OWNER);
            String title = (String) roomMap.get(RedisSocket.REDIS_ROOM_TITLE);
            String max = (String) roomMap.get(RedisSocket.REDIS_ROOM_MAX);
            String admin_1 = (String) roomMap.get(RedisSocket.REDIS_ROOM_ADMINISTRATORS_1);
            String admin_2 = (String) roomMap.get(RedisSocket.REDIS_ROOM_ADMINISTRATORS_2);
            String like = String.valueOf(roomMap.get(RedisSocket.REDIS_ROOM_LIKE));
            String cover = (String) roomMap.get(RedisSocket.REDIS_ROOM_IMAGE_COVER);
            String background = (String) roomMap.get(RedisSocket.REDIS_ROOM_IMAGE_BACKGROUND);
            String other_1 = (String) roomMap.get(RedisSocket.REDIS_ROOM_OTHER_1);
            String other_2 = (String) roomMap.get(RedisSocket.REDIS_ROOM_OTHER_2);
            Map<String, String> roomInfo = new HashMap<>();
            roomInfo.put(RedisSocket.REDIS_ROOM_PASSWORD, password);
            roomInfo.put(RedisSocket.REDIS_ROOM_OWNER, owner);
            roomInfo.put(RedisSocket.REDIS_ROOM_TITLE, title);
            roomInfo.put(RedisSocket.REDIS_ROOM_MAX, max);
            roomInfo.put(RedisSocket.REDIS_ROOM_ADMINISTRATORS_1, admin_1);
            roomInfo.put(RedisSocket.REDIS_ROOM_ADMINISTRATORS_2, admin_2);
            roomInfo.put(RedisSocket.REDIS_ROOM_LIKE, like);
            roomInfo.put(RedisSocket.REDIS_ROOM_IMAGE_COVER, cover);
            roomInfo.put(RedisSocket.REDIS_ROOM_IMAGE_BACKGROUND, background);
            roomInfo.put(RedisSocket.REDIS_ROOM_OTHER_1, other_1);
            roomInfo.put(RedisSocket.REDIS_ROOM_OTHER_2, other_2);
            info.add(roomInfo);
            roomMap.remove(RedisSocket.REDIS_ROOM_PASSWORD);
            roomMap.remove(RedisSocket.REDIS_ROOM_OWNER);
            roomMap.remove(RedisSocket.REDIS_ROOM_TITLE);
            roomMap.remove(RedisSocket.REDIS_ROOM_MAX);
            roomMap.remove(RedisSocket.REDIS_ROOM_ADMINISTRATORS_1);
            roomMap.remove(RedisSocket.REDIS_ROOM_ADMINISTRATORS_2);
            roomMap.remove(RedisSocket.REDIS_ROOM_LIKE);
            roomMap.remove(RedisSocket.REDIS_ROOM_IMAGE_COVER);
            roomMap.remove(RedisSocket.REDIS_ROOM_IMAGE_BACKGROUND);
            roomMap.remove(RedisSocket.REDIS_ROOM_OTHER_1);
            roomMap.remove(RedisSocket.REDIS_ROOM_OTHER_2);
            Map<String, String> roomOnline = new HashMap<>();
            Map<String, String> roomNotOnline = new HashMap<>();
            roomMap.forEach((key, value) -> {
                if (value.equals("1")) {
                    roomOnline.put((String) key, "1");
                } else if (value.equals("0")) {
                    roomNotOnline.put((String) key, "0");
                }
            });
            info.add(roomOnline);
            info.add(roomNotOnline);
            return info;
        }
    }
}
