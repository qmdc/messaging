package com.qiandao.messagingcore.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.qiandao.messagingcommon.enumConstant.CoreEnum;
import com.qiandao.messagingcommon.utils.R;
import com.qiandao.messagingcore.feign.AuthFeignService;
import com.qiandao.messagingcore.service.InfoServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/core/info/")
@Api(tags = "Info")
public class InfoController {

    @Autowired
    private InfoServer infoServer;

    @Autowired
    private AuthFeignService authFeignService;

    @ApiOperation(httpMethod = "GET", value = "获取所有在线用户ID")
    @SaCheckRole(value = {"user", "admin", "root"}, mode = SaMode.OR)
    @GetMapping("/getAllOnlineUser")
    public R getAllOnlineUser() {
        List<String> allOnlineUser = infoServer.getAllOnlineUser();
        return R.ok().put("data", allOnlineUser);
    }

    @ApiOperation(httpMethod = "GET", value = "获取两用户之间的历史聊天记录")
    @SaCheckLogin
    @GetMapping("/getUserMsg/{fromId}/{toId}")
    public R getUserMsg(@PathVariable("fromId") String fromId, @PathVariable("toId") String toId) {
        //确保当前用户只能拿到当前用户与其他用户的聊天信息
        String loginId = (String) StpUtil.getLoginId();
        R no = authFeignService.getNoByFromId(loginId);
        if (no.getCode().equals("00000")) {
            String data = (String) no.get("data");
            if (data.equals(fromId)) {
                Map<String, String> userMsg = infoServer.getUserMsg(fromId, toId); //TODO 按日期分页加载3天
                return R.ok().put("data", userMsg);
            }
            return R.error("非当前用户操作,无法获取他人之间聊天记录");
        }
        return R.error(CoreEnum.CORE_ENUM_RPC_FAIL.getCode(), CoreEnum.CORE_ENUM_RPC_FAIL.getMsg());
    }

    @ApiOperation(httpMethod = "GET", value = "获取所有房间列表及详细情况")
    @SaCheckLogin
    @GetMapping("/getAllRoom")
    public R getAllRoom() {
        List<Map<String, String>> allRooms = infoServer.getAllRoom();
        return R.ok().put("data", allRooms);
    }

    //判断是否成功加入当前房间

    @ApiOperation(httpMethod = "GET", value = "根据房间id获取房间内所有信息")
    @SaCheckLogin
    @GetMapping("/getRoomInfo")
    public R getRoomInfo(@RequestParam("roomId") String roomId) {
        List<Map<String, String>> roomInfo = infoServer.getRoomInfoById(roomId);
        return R.ok().put("data", roomInfo);
    }

    @ApiOperation(httpMethod = "GET", value = "根据用户id批量返回用户的头像信息")
    @SaCheckLogin
    @GetMapping("/getUserImage")
    public R getUserImage(@RequestBody Map<String, String> users) {
        R images = authFeignService.getUserImage(users);
        if (images.getCode().equals("00000")) {
            return R.ok().put("data", images);
        } else {
            return R.error(CoreEnum.CORE_ENUM_RPC_FAIL.getCode(), CoreEnum.CORE_ENUM_RPC_FAIL.getMsg());
        }
    }

    @ApiOperation(httpMethod = "GET", value = "根据房间id获取房间内历史消息")   //TODO 按条数分页
    @GetMapping("/roomMsg/{fromId}/{roomId}")
    public R getRoomMsg(@PathVariable("fromId") String fromId, @PathVariable("roomId") String roomId) {
        //TODO 判断fromId是否在当前房间内
        Map<String, String> map = infoServer.getRoomMsg(roomId);
        return R.ok().put("data", map);
    }
}
