package com.qiandao.messagingcore.service.impl;

import com.qiandao.messagingcore.constant.CoreConstant;
import com.qiandao.messagingcore.feign.AuthFeignService;
import com.qiandao.messagingcore.service.UserTabooServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserTabooServerImpl implements UserTabooServer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AuthFeignService authFeignService;

    @Override
    public boolean taboo(String roomId, String userId, String time, String msg) {
        String key = CoreConstant.REDIS_ROOM_TABOO + roomId + "_" + userId;
        //远程插入用户违规记录
//        String message = roomId + "_" + userId + "_" + time + "_" + msg;
//        R r = authFeignService.setUserTaboo(message, userId);
//        if (!Objects.equals(r.getCode(), "00000")) {
//            return false;
//        }
        stringRedisTemplate.opsForValue().set(key, msg, Long.parseLong(time), TimeUnit.SECONDS);
        return true;
    }

}
