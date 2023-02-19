package com.qiandao.messagingauththird.controller;

import com.qiandao.messagingauththird.service.AuthByPasswdService;
import com.qiandao.messagingcommon.utils.R;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/feign")
public class SatokenFeignController {

    @Autowired
    private AuthByPasswdService authByPasswdService;

    @ApiOperation("提供远程获取权限列表方法")
    @RequestMapping("/perm/list")
    public R getPermList(String loginId) {
        List<String> permList = authByPasswdService.getPermList(loginId);
        return R.ok().put("data",permList);
    }

    @ApiOperation("提供远程获取角色列表方法")
    @RequestMapping("/role/list")
    public R getRoleList(String loginId) {
        List<String> roleList = authByPasswdService.getRoleList(loginId);
        return R.ok().put("data",roleList);
    }

    @ApiOperation("提供远程根据loginId获取no的方法")
    @RequestMapping("/loginId/getNo")
    public R getNoByFromId(String loginId) {
        String phone = authByPasswdService.getNoByFromId(loginId);
        return R.ok().put("data",phone);
    }

    @ApiOperation("提供远程根据no获取用户头像的方法")
    @RequestMapping("/user/image")
    public R getUserImage(@RequestBody Map<String, String> users) {
        Map<String,String> images = authByPasswdService.getUserImageByNo(users);
        return R.ok().put("data",images);
    }

    @ApiOperation("提供远程添加用户违规记录的方法")
    @RequestMapping("/user/taboo")
    public R setUserTaboo(String messages, String userId) {
        int i = authByPasswdService.insertUserTaboo(messages,userId);
        if (i > 0) {
            return R.ok();
        }
        return R.error("添加异常");
    }


}
