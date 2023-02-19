package com.qiandao.messagingcore.core.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.qiandao.messagingcommon.enumConstant.CoreEnum;
import com.qiandao.messagingcommon.simpleUtils.SimpleDate;
import com.qiandao.messagingcommon.utils.R;
import com.qiandao.messagingcore.core.vo.MessageBody;
import com.qiandao.messagingcore.feign.AuthFeignService;
import com.qiandao.messagingcore.core.service.WebSocketServerInterface;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.http.WebSocket;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/core/web/")
@Api(tags = "WebSocket")
public class WebSocketController {

    private WebSocket webSocket;

    @Autowired
    private WebSocketServerInterface webSocketServerInterface;

    @Autowired
    private AuthFeignService authFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${colony.id}")
    private String colonyId;

    @Value("${colony.take}")
    private String[] colonyTake;

    public String getColonyId() {
        return colonyId;
    }

    public List<String> getColonyTake() {
        return Arrays.stream(colonyTake).toList();
    }

    @Autowired
    Environment environment;

    public String getServerPort() {
        return environment.getProperty("local.server.port");
    }


    @ApiOperation(httpMethod = "POST", value = "发送广播信息给所有在线用户")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    @PostMapping("/sendAllWebSocket")
    public R sendAll(@RequestParam String text) {
        log.info("发送广播啦：{}", text);
        MessageBody messageBody = new MessageBody();
        messageBody.setFromName("千岛");
        messageBody.setContent(text);
        messageBody.setSendTime(SimpleDate.nowtime());
        String msg = JSON.toJSONString(messageBody);
        webSocketServerInterface.sendAllMessagePub(msg);
        return R.ok().put("data", messageBody);
    }

    /**
     * @deprecated
     */
    @ApiOperation(httpMethod = "GET", value = "当前用户(管理员)发送信息给指定用户")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    @GetMapping("/sendOneWebSocket/{fromId}/{toId}")
    public R sendOneWebSocket(@RequestBody MessageBody messageBody, @PathVariable("fromId") String fromId, @PathVariable("toId") String toId) {
        //确保是当前用户发送给其他人
        String loginId = (String) StpUtil.getLoginId();
        R no = authFeignService.getNoByFromId(loginId);
        if (no.getCode().equals("00000")) {
            String data = (String) no.get("data");
            if (data.equals(fromId)) {
                String content = messageBody.getContent();
                String message = JSON.toJSONString(messageBody);
                webSocketServerInterface.sendMessageByUserSub(fromId, toId, SimpleDate.nowtime(), content, message);
                return R.ok();
            }
            return R.error("非当前用户操作,无法冒用他人信息");
        }
        return R.error(CoreEnum.CORE_ENUM_RPC_FAIL.getCode(), CoreEnum.CORE_ENUM_RPC_FAIL.getMsg());
    }

    //TODO 当前用户推送消息到指定批量用户

}
