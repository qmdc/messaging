package com.qiandao.messagingcore.feign;

import com.qiandao.messagingcommon.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient("messaging-gateway")
public interface AuthFeignService {

    @RequestMapping("/api/auth/feign/perm/list")
    R getPermList(@RequestParam("loginId") String loginId);

    @RequestMapping("/api/auth/feign/role/list")
    R getRoleList(@RequestParam("loginId") String loginId);

    @RequestMapping("/api/auth/feign/loginId/getNo")
    R getNoByFromId(@RequestParam("loginId") String loginId);

    @RequestMapping("/api/auth/feign/user/image")
    R getUserImage(@RequestBody Map<String, String> users);

    @RequestMapping("/user/taboo")
    R setUserTaboo(@RequestParam("messages") String messages, @RequestParam("userId") String userId);
}
