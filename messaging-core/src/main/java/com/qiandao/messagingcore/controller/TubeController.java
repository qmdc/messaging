package com.qiandao.messagingcore.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.qiandao.messagingcommon.enumConstant.CoreEnum;
import com.qiandao.messagingcommon.utils.R;
import com.qiandao.messagingcore.core.constant.RedisSocket;
import com.qiandao.messagingcore.service.UserTabooServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/core/tube/")
@Api(tags = "Tube")
public class TubeController {

    @Autowired
    private UserTabooServer userTabooServer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @ApiOperation("房主管理员禁言违规用户")
    @SaCheckLogin
    @RequestMapping("/user/taboo")
    public R tabooUser(@RequestParam("userId") String userId,
                       @RequestParam("roomId") String roomId,
                       @RequestParam("time") String time,
                       @RequestParam("msg") String msg) {
        //确定当前操作用户为当前房间房主或管理员
        String loginOwner = (String) StpUtil.getLoginIdDefaultNull();
        String roomRedis = stringRedisTemplate.opsForValue().get(RedisSocket.REDIS_ROOM_SOCKET + ":" + roomId);
        JSONObject jsonObject = JSON.parseObject(roomRedis);
        boolean flag = false;
        if (jsonObject != null) {
            String ownerId = (String) jsonObject.get("owner");
            String administrators_1 = (String) jsonObject.get("administrators_1");
            String administrators_2 = (String) jsonObject.get("administrators_2");
            if (ownerId.equals(loginOwner)) {
                flag = true;
            } else if (administrators_1.equals(loginOwner)) {
                flag = true;
            } else if (administrators_2.equals(loginOwner)) {
                flag = true;
            }
        }
        if (!flag) {
            return R.error(CoreEnum.CORE_ENUM_ADMIN_NO.getCode(), CoreEnum.CORE_ENUM_ADMIN_NO.getMsg());
        }
        boolean taboo = userTabooServer.taboo(roomId, userId, time, msg);
        if (!taboo) {
            return R.error(CoreEnum.CORE_ENUM_TABOO_FAIL.getCode(), CoreEnum.CORE_ENUM_TABOO_FAIL.getMsg());
        }
        return R.ok();
    }

    @ApiOperation("系统管理员踢违规用户下线")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    @RequestMapping("/user/offline")
    public R offline(@RequestParam("userId") String userId,
                       @RequestParam("time") String time,
                       @RequestParam("msg") String msg) {
        return null;
    }
}
