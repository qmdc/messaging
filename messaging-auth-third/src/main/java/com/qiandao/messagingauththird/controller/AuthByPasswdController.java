package com.qiandao.messagingauththird.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiandao.messagingauththird.constant.AuthConstant;
import com.qiandao.messagingauththird.entity.UserInfoEntity;
import com.qiandao.messagingauththird.service.AuthByPasswdService;
import com.qiandao.messagingauththird.utils.AesUtil;
import com.qiandao.messagingcommon.enumConstant.AuthEnum;
import com.qiandao.messagingcommon.utils.R;
import com.qiandao.messagingcommon.utils.RRException;
import com.qiandao.messagingcommon.valid.AddGroup;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/auth/passwd")
@Api(tags = "AuthByPasswd")
public class AuthByPasswdController {

    @Autowired
    private AuthByPasswdService authByPasswdService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AesUtil aesUtil;

    @ApiOperation(httpMethod = "POST", value = "通过账号、密码登录")
    @PostMapping("/login/byPasswd")
    public R userLoginPasswd(@RequestParam String userId, @RequestParam String password) {
        QueryWrapper<UserInfoEntity> wrapper = new QueryWrapper<>();
        UserInfoEntity userInfoEntity = authByPasswdService.getOne(wrapper.eq("userId", userId));
        if (userInfoEntity == null) {
            return R.error(AuthEnum.AUTH_ENUM_NAME_FAIL.getCode(), AuthEnum.AUTH_ENUM_NAME_FAIL.getMsg());
        }
        String passwd = userInfoEntity.getPassword();
        String aesPlaintext;
        try {
            aesPlaintext = aesUtil.getAesPlaintext(userId, password, passwd);
        } catch (Exception e) {
            return R.error(AuthEnum.AUTH_ENUM_PASSWD_FAIL.getCode(), AuthEnum.AUTH_ENUM_PASSWD_FAIL.getMsg());
        }
        if (password.equals(aesPlaintext)) {
            StpUtil.login(userInfoEntity.getPhone());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            return R.ok().put("data", tokenInfo);
        }
        return R.error(AuthEnum.AUTH_ENUM_PASSWD_FAIL.getCode(), AuthEnum.AUTH_ENUM_PASSWD_FAIL.getMsg());
    }

    @ApiOperation(httpMethod = "POST", value = "通过手机号登录")
    @PostMapping("/login/byPhone")
    public R userLoginByPhone(@RequestParam String phone, @RequestParam String code) {
        //TODO 判断是否注册过，否则优先注册
        String codeRedis = stringRedisTemplate.opsForValue().get(AuthConstant.loginSmsPre + phone);
        if (Objects.equals(codeRedis, code)) {
            //验证通过
            StpUtil.login(phone);
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            return R.ok().put("data", tokenInfo);
        } else {
            return R.error(AuthEnum.AUTH_ENUM_PHONE_ERROR.getCode(), AuthEnum.AUTH_ENUM_PHONE_ERROR.getMsg() + "或已过期");
        }
    }

    @ApiOperation(httpMethod = "POST", value = "用户注册,必填参数:【userId,password,phone,code】选填参数:【userName,userHead,userSex】")
    @PostMapping("/register")
    public R userRegister(@Validated(AddGroup.class) @RequestBody UserInfoEntity userInfo) {
        System.err.println(userInfo);   //解析出 用户id 用户密码 用户头像(可能存在) 用户昵称(可能存在)
        //判断该用户id与手机号是否可用
        UserInfoEntity userId = authByPasswdService.getOne(new QueryWrapper<UserInfoEntity>().eq("userId", userInfo.getUserId()));
        if (userId != null) {
            return R.error(AuthEnum.AUTH_ENUM_REGISTER_EXISTENCE.getCode(), AuthEnum.AUTH_ENUM_REGISTER_EXISTENCE.getMsg());
        }
        UserInfoEntity phone = authByPasswdService.getOne(new QueryWrapper<UserInfoEntity>().eq("phone", userInfo.getPhone()));
        if (phone != null) {
            return R.error(AuthEnum.AUTH_ENUM_PHONE_EXISTENCE.getCode(), AuthEnum.AUTH_ENUM_PHONE_EXISTENCE.getMsg());
        }
        //根据账号密码得到盐值与密钥一起加密密码得到密文
        String aesCiphertext;
        try {
            aesCiphertext = aesUtil.getAesCiphertext(userInfo.getUserId(), userInfo.getPassword());
        } catch (RRException e) {
            log.error(e.getMessage());
            return R.error(AuthEnum.AUTH_ENUM_LEN_SHORT.getCode(), AuthEnum.AUTH_ENUM_LEN_SHORT.getMsg());
        }
        UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setUserId(userInfo.getUserId());
        userInfoEntity.setPassword(aesCiphertext);
        //处理头像与昵称、性别
        if (StrUtil.isNotEmpty(userInfo.getUserHead())) {
            userInfoEntity.setUserHead(userInfo.getUserHead());
        }
        if (StrUtil.isNotEmpty(userInfo.getUserName())) {
            userInfoEntity.setUserName(userInfo.getUserName());
        }
        if (userInfo.getUserSex() != null) {
            userInfoEntity.setUserSex(userInfo.getUserSex());
        }
        //处理注册短信验证码
        String code = stringRedisTemplate.opsForValue().get(AuthConstant.registerSmsPre + userInfo.getPhone());
        if (StrUtil.isEmpty(code)) {
            return R.error(AuthEnum.AUTH_ENUM_PHONE_OVERTIME.getCode(), AuthEnum.AUTH_ENUM_PHONE_OVERTIME.getMsg() + "或尚未发送验证码");
        }
        if (!code.equals(userInfo.getCode())) {
            return R.error(AuthEnum.AUTH_ENUM_PHONE_ERROR.getCode(), AuthEnum.AUTH_ENUM_PHONE_ERROR.getMsg());
        }
        userInfoEntity.setPhone(userInfo.getPhone());
        //验证通过,插入数据库
        boolean register = authByPasswdService.insertUser(userInfoEntity);
        if (register) {
            log.info("新用户注册成功:{}", JSON.toJSONString(userInfoEntity));
            stringRedisTemplate.delete(AuthConstant.registerSmsPre + userInfo.getPhone());
            return R.ok();
        } else {
            log.info("新用户注册失败:{}", JSON.toJSONString(userInfoEntity));
            stringRedisTemplate.delete(AuthConstant.registerSmsPre + userInfo.getPhone());
            return R.error(AuthEnum.AUTH_ENUM_REGISTER_FAIL.getCode(), AuthEnum.AUTH_ENUM_REGISTER_FAIL.getMsg());
        }
    }

    @ApiOperation(httpMethod = "POST", value = "修改密码")
    @SaCheckLogin
    @PostMapping("/modify/passwd")
    public R userModifyPasswd(@RequestParam String oldPasswd, @RequestParam String newPasswd) {
        if (newPasswd.length() >= 16 || newPasswd.length() <= 8) {
            return R.error(AuthEnum.AUTH_ENUM_LEN_FAIL.getCode(), AuthEnum.AUTH_ENUM_LEN_FAIL.getMsg());
        }
        boolean flag = authByPasswdService.userModifyPasswd(oldPasswd, newPasswd);
        if (flag) {
            return R.ok();
        } else {
            return R.error("密码修改失败");
        }
    }

    @ApiOperation(httpMethod = "POST", value = "通过手机号找回密码(重设密码)")
    @PostMapping("/retrieve/passwd")
    public R retrievePasswd(@RequestParam String phone, @RequestParam String code, @RequestParam String newPasswd) {
        if (newPasswd.length() >= 16 || newPasswd.length() <= 8) {
            return R.error(AuthEnum.AUTH_ENUM_LEN_FAIL.getCode(), AuthEnum.AUTH_ENUM_LEN_FAIL.getMsg());
        }
        String codeRedis = stringRedisTemplate.opsForValue().get(AuthConstant.retrieveSmsPre + phone);
        if (Objects.equals(codeRedis, code)) {
            try {
                authByPasswdService.retrievePasswd(phone, newPasswd);
            } catch (RRException e) {
                return R.error(AuthEnum.AUTH_ENUM_PHONE_UNREGISTERED.getCode(), AuthEnum.AUTH_ENUM_PHONE_UNREGISTERED.getMsg());
            } catch (Exception e) {
                log.info("通过手机号找回密码失败:手机号{},新密码{}",phone,newPasswd);
                return R.error();
            }
            return R.ok();
        } else {
            return R.error(AuthEnum.AUTH_ENUM_PHONE_ERROR.getCode(), AuthEnum.AUTH_ENUM_PHONE_ERROR.getMsg() + "或已过期");
        }
    }

    //TODO 用户修改手机号，要用@CacheEvict(value = "no", key = "#loginId")移除缓存的loginId信息

    @GetMapping("/role")
    @SaCheckRole("root")
    public R getRole() {
        HashMap<String, Object> map = new HashMap<>();
        boolean login = StpUtil.isLogin();
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        SaSession session = StpUtil.getSessionByLoginId("18726740474");
        long tokenActivityTimeout = StpUtil.getTokenActivityTimeout();
        List<String> roleList = StpUtil.getRoleList();
        List<String> permissionList = StpUtil.getPermissionList();
        map.put("login", login);
        map.put("tokenInfo", tokenInfo);
        map.put("session", session);
        map.put("tokenActivityTimeout", tokenActivityTimeout);
        map.put("roleList", roleList);
        map.put("permissionList", permissionList);
        System.err.println(map);
        return R.ok(map);
    }

    @GetMapping("perm")
    @SaCheckPermission("a")
    public R perm() {
        List<String> roleList = StpUtil.getRoleList();
        List<String> permissionList = StpUtil.getPermissionList();
        HashMap<String, Object> map = new HashMap<>();
        map.put("roleList", roleList);
        map.put("permissionList", permissionList);
        System.err.println(map);
        return R.ok(map);
    }
}
