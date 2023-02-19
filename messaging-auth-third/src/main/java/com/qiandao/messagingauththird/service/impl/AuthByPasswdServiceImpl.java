package com.qiandao.messagingauththird.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiandao.messagingauththird.constant.AuthConstant;
import com.qiandao.messagingauththird.dao.AuthByPasswdDao;
import com.qiandao.messagingauththird.entity.UserInfoEntity;
import com.qiandao.messagingauththird.service.AuthByPasswdService;
import com.qiandao.messagingauththird.service.SendSms;
import com.qiandao.messagingauththird.utils.AesUtil;
import com.qiandao.messagingcommon.simpleUtils.SimpleDate;
import com.qiandao.messagingcommon.simpleUtils.RandomUtil;
import com.qiandao.messagingcommon.utils.RRException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthByPasswdServiceImpl extends ServiceImpl<AuthByPasswdDao, UserInfoEntity> implements AuthByPasswdService {

    @Autowired
    private SendSms sendSms;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AesUtil aesUtil;

    @Value("${tencent-sms.register_templateId}")
    private String register_templateId;

    @Value("${tencent-sms.retrieve_templateId}")
    private String retrieve_templateId;

    @Value("${tencent-sms.login_templateId}")
    private String login_templateId;

    @Override
    @Cacheable(value = "perm",key = "#loginId")
    public List<String> getPermList(String loginId) {
        log.info("查询了数据库-权限！！{}",loginId);
        UserInfoEntity userInfoEntity = baseMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("phone",loginId));
        String perm = userInfoEntity.getPerm();
        List<String> perms = null;
        if (StrUtil.isNotEmpty(perm)) {
            String[] strings = perm.split("\\|");
            perms = Arrays.stream(strings).toList();
        }
        return perms;
    }

    @Override
    @Cacheable(value = "role",key = "#loginId")     //最终key的形式：【CACHE:role:[loginId]】:【value】
    public List<String> getRoleList(String loginId) {
        log.info("查询了数据库-角色！！{}",loginId);
        UserInfoEntity userInfoEntity = baseMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("phone",loginId));
        String role = userInfoEntity.getRole();
        List<String> roles = null;
        if (StrUtil.isNotEmpty(role)) {
            String[] strings = role.split("\\|");
            roles = Arrays.stream(strings).toList();
        }
        return roles;
    }

    @Override
    public String sendSmsPattern(String phone, String templateId) {
        String[] phones = {phone};
        String verificationCode = null;
        //根据发信模式获取相应缓存验证码值
        if (templateId.equals(register_templateId)) {
            verificationCode = stringRedisTemplate.opsForValue().get(AuthConstant.registerSmsPre + phone);
        } else if (templateId.equals(retrieve_templateId)) {
            verificationCode = stringRedisTemplate.opsForValue().get(AuthConstant.retrieveSmsPre + phone);
        } else if (templateId.equals(login_templateId)) {
            verificationCode = stringRedisTemplate.opsForValue().get(AuthConstant.loginSmsPre + phone);
        }
        //如果为空则发送短信
        if (StrUtil.isEmpty(verificationCode)) {
            String code = RandomUtil.getCode();
            boolean send;
            if (templateId.equals(register_templateId)) {
                send = sendSms.send(phones, code, templateId);
                log.info("{} 向 {} 发送了注册短信，内容是 {}", SimpleDate.nowtime(), phone, code);
                if (send) {
                    log.info("注册短信发送成功");
                    stringRedisTemplate.opsForValue().set(AuthConstant.registerSmsPre + phone, code, 5, TimeUnit.MINUTES);
                    return "a"; //发送成功
                }else {
                    return "b"; //短信发送异常
                }
            } else if (templateId.equals(retrieve_templateId)) {
                send = sendSms.send(phones, code, templateId);
                log.info("{} 向 {} 发送了找回密码短信，内容是 {}", SimpleDate.nowtime(), phone, code);
                if (send) {
                    log.info("找回密码短信发送成功");
                    stringRedisTemplate.opsForValue().set(AuthConstant.retrieveSmsPre + phone, code, 3, TimeUnit.MINUTES);
                    return "a"; //发送成功
                }else {
                    return "b"; //短信发送异常
                }
            } else if (templateId.equals(login_templateId)) {
                send = sendSms.send(phones, code, templateId);
                log.info("{} 向 {} 发送了登录短信，内容是 {}", SimpleDate.nowtime(), phone, code);
                if (send) {
                    log.info("登录短信发送成功");
                    stringRedisTemplate.opsForValue().set(AuthConstant.loginSmsPre + phone, code, 2, TimeUnit.MINUTES);
                    return "a"; //发送成功
                }else {
                    return "b"; //短信发送异常
                }
            }
            return "000";   //发信模式错误
        } else {
            return "c";     //上次发送的验证码还在有效期内
        }
    }

    @Override
    public boolean insertUser(UserInfoEntity userInfoEntity) {
        int insert = baseMapper.insert(userInfoEntity);
        return insert > 0;
    }

    @Override
    public boolean userModifyPasswd(String oldPasswd, String newPasswd) {
        String loginId = (String) StpUtil.getLoginIdDefaultNull();
        UserInfoEntity userId = baseMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("phone", loginId));
        if (userId == null) {
            return false;
        }
        String password = userId.getPassword();
        System.out.println("老密码："+password);
        String aesPlaintext;
        try {
            aesPlaintext = aesUtil.getAesPlaintext(userId.getUserId(), oldPasswd, password);
        } catch (Exception e) {
            log.info("旧密码错误");
            return false;
        }
        if (aesPlaintext.equals(oldPasswd)) {
            String aesCiphertext = aesUtil.getAesCiphertext(userId.getUserId(), newPasswd);
            System.out.println("新密码："+aesCiphertext);
            LambdaUpdateWrapper<UserInfoEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(UserInfoEntity::getPassword,aesCiphertext).eq(UserInfoEntity::getUserId,userId.getUserId());
            this.update(updateWrapper);
            return true;
        }
        return false;
    }

    @Override
    public void retrievePasswd(String phone,String newPasswd) {
        UserInfoEntity userId = baseMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("phone", phone));
        if (userId == null) {
            throw new RRException("该手机号尚未注册");
        }
        String aesCiphertext = aesUtil.getAesCiphertext(userId.getUserId(), newPasswd);
        LambdaUpdateWrapper<UserInfoEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(UserInfoEntity::getPassword,aesCiphertext).eq(UserInfoEntity::getPhone,phone);
        this.update(wrapper);
    }

    @Override
    @Cacheable(value = "no",key = "#loginId")
    public String getNoByFromId(String loginId) {
        log.info("查询数据库no信息");
        UserInfoEntity userInfo = baseMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("phone", loginId));
        if (userInfo != null) {
            return String.valueOf(userInfo.getNo());
        }
        return null;
    }

    @Override
    public Map<String, String> getUserImageByNo(Map<String, String> users) {
        List<String> userList = new ArrayList<>();
        users.forEach((key,value)-> userList.add(key));
        List<UserInfoEntity> userInfoEntities = baseMapper.selectBatchIds(userList);
        Map<String, String> map = new HashMap<>();
        userInfoEntities.forEach(item->{
            Long no = item.getNo();
            String userHead = item.getUserHead();
            map.put(String.valueOf(no),userHead);
        });
        return map;
    }

    @Override
    public int insertUserTaboo(String messages,String no) {
        UserInfoEntity userInfo = baseMapper.selectById(no);
        String history = userInfo.getViolationRecord();
        String newMsg;
        if (history == null) {
            newMsg = messages;
        }else {
            newMsg = history + "|" +messages.replace("|", "");
        }
        userInfo.setViolationRecord(newMsg);
        return baseMapper.update(userInfo,new QueryWrapper<UserInfoEntity>().eq("no",no));
    }
}
