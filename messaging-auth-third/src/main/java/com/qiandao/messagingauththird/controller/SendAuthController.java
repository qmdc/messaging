package com.qiandao.messagingauththird.controller;

import cn.hutool.core.util.StrUtil;
import com.qiandao.messagingauththird.constant.AuthConstant;
import com.qiandao.messagingauththird.service.AuthByPasswdService;
import com.qiandao.messagingcommon.enumConstant.AuthEnum;
import com.qiandao.messagingcommon.simpleUtils.PatternUtil;
import com.qiandao.messagingcommon.simpleUtils.RandomUtil;
import com.qiandao.messagingcommon.utils.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/auth/send")
@Api(tags = "ASendAuth")
public class SendAuthController {

    @Autowired
    private AuthByPasswdService authByPasswdService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    JavaMailSenderImpl mailSender;

    @Value("${tencent-sms.register_templateId}")
    private String register_templateId;

    @Value("${tencent-sms.retrieve_templateId}")
    private String retrieve_templateId;

    @Value("${tencent-sms.login_templateId}")
    private String login_templateId;

    /**
     * 短信发送三种模式
     * @param phone 手机号
     * @param pattern 三种发信模式
     *                注册：register
     *                找回：retrieve
     *                登录：login
     * @return R
     */
    @ApiOperation(httpMethod = "GET", value = "发送短信验证码【注册】【登录】【重置】")
    @GetMapping("/sendSmsCode")
    public R sendSmsPattern(@RequestParam String phone, @RequestParam String pattern) {
        boolean mobileNO = PatternUtil.isMobileNO(phone);
        String flag;
        if (mobileNO) {
            switch (pattern) {
                case "register" ->
                        //注册
                        flag = authByPasswdService.sendSmsPattern(phone, register_templateId);
                case "retrieve" ->
                        //找回
                        flag = authByPasswdService.sendSmsPattern(phone, retrieve_templateId);
                case "login" ->
                        //登录
                        flag = authByPasswdService.sendSmsPattern(phone, login_templateId);
                default -> flag = "000";
            }
        } else {
            return R.error(AuthEnum.AUTH_ENUM_PHONE_FAIL.getCode(), AuthEnum.AUTH_ENUM_PHONE_FAIL.getMsg());
        }
        if (flag.equals("000")) {
            return R.error("发信模式错误");
        }
        return switch (flag) {
            case "a" -> R.ok();
            case "b" -> R.error("短信发送异常，请重试!");
            case "c" -> R.error(AuthEnum.AUTH_ENUM_PHONE_REPEAT.getCode(), AuthEnum.AUTH_ENUM_PHONE_REPEAT.getMsg() + ",上次验证码在有效期内");
            default -> R.error();
        };
    }

    @ApiOperation(httpMethod = "GET", value = "发送邮箱验证码")
    @GetMapping("/sendEmailCode")
    public R sendEmailCode(@RequestParam String email){
        boolean flag = PatternUtil.isEmail(email);
        if (!flag) {
            return R.error(AuthEnum.AUTH_ENUM_EMAIL_ILLEGAL.getCode(), AuthEnum.AUTH_ENUM_EMAIL_ILLEGAL.getMsg());
        }
        String history = stringRedisTemplate.opsForValue().get(AuthConstant.emailPre + email);
        if (StrUtil.isNotEmpty(history)) {
            return R.error(AuthEnum.AUTH_ENUM_EMAIL_REPEAT.getCode(), AuthEnum.AUTH_ENUM_EMAIL_REPEAT.getMsg());
        }
        String uuid = RandomUtil.getUUID(5);
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("即时通讯");     //邮件标题
        mailMessage.setText("您的邮箱验证码是: "+uuid+" 请于5分钟内填写");    //邮件内容
        stringRedisTemplate.opsForValue().set(AuthConstant.emailPre+email,uuid,5, TimeUnit.MINUTES);
        String[] users = {email};
        mailMessage.setFrom("brids_9642@sina.cn");
        mailMessage.setTo(users);       //可以群发
        try {
            mailSender.send(mailMessage);   //发送
        } catch (MailException e) {
            log.error("邮箱验证码发送失败，发送至{},内容为{}",email,uuid);
            stringRedisTemplate.delete(AuthConstant.emailPre+email);
            return R.error(AuthEnum.AUTH_ENUM_EMAIL_FAIL.getCode(),AuthEnum.AUTH_ENUM_EMAIL_FAIL.getMsg());
        }
        log.info("邮箱验证码发送成功，发送至{},内容为{}",email,uuid);
        return R.ok();
    }
}
