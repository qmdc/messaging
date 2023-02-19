package com.qiandao.messagingauththird.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qiandao.messagingauththird.constant.AuthConstant;
import com.qiandao.messagingauththird.entity.UserInfoEntity;
import com.qiandao.messagingauththird.service.AuthByPasswdService;
import com.qiandao.messagingauththird.vo.SocialUser;
import com.qiandao.messagingcommon.enumConstant.AuthEnum;
import com.qiandao.messagingcommon.simpleUtils.PatternUtil;
import com.qiandao.messagingcommon.utils.HttpUtils;
import com.qiandao.messagingcommon.utils.R;
import com.qiandao.messagingcommon.utils.RRException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/other")
@Api(tags = "OAuth2")
public class OAuth2Controller {

    @Autowired
    private AuthByPasswdService authByPasswdService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${weibo.client_id}")
    private String client_id;
    @Value("${weibo.client_secret}")
    private String client_secret;
    @Value("${weibo.grant_type}")
    private String grant_type;
    @Value("${weibo.redirect_uri}")
    private String redirect_uri;

    @Value("${gitee.client_id}")
    public String client_id_gitee;
    @Value("${gitee.client_secret}")
    public String client_secret_gitee;
    @Value("${gitee.grant_type}")
    private String grant_type_gitee;
    @Value("${gitee.redirect_uri}")
    public String redirect_uri_gitee;

    @Value("${github.client_id}")
    public String client_id_github;
    @Value("${github.client_secret}")
    private String client_secret_github;
    @Value("${github.redirect_uri}")
    public String redirect_uri_github;

    /**
     * 微博登录 https://api.weibo.com/oauth2/authorize?client_id=719034544&response_type=code&redirect_uri=http://messaging.cn/api/auth/other/oauth2.0/weibo/success
     *
     * @param code 回调code码
     * @return R
     */
    @ApiOperation(httpMethod = "GET", value = "微博登录或绑定")
    @GetMapping("/oauth2.0/weibo/success")
    public R weibo(@RequestParam("code") String code) {
        log.info("请求微博登录code:" + code);
        Map<String, String> header = new HashMap<>();
        Map<String, String> query = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("client_id", client_id);
        map.put("client_secret", client_secret);
        map.put("grant_type", grant_type);
        map.put("redirect_uri", redirect_uri);
        map.put("code", code);
        //根据code换取access_token与uid（目的就是要uid作为用户的唯一标识，没必要再用token获取用户的其他信息）
        HttpResponse response;
        try {
            response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", header, query, map);
        } catch (Exception e) {
            log.info("微博接口调用异常：{}", e.getMessage());
            return R.error(AuthEnum.AUTH_ENUM_WEIBO_FAIL.getCode(), AuthEnum.AUTH_ENUM_WEIBO_FAIL.getMsg());
        }

        if (response.getStatusLine().getStatusCode() == 200) {
            String json;
            try {
                json = EntityUtils.toString(response.getEntity());
            } catch (IOException e) {
                log.info(e.getMessage());
                return R.error();
            }
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            String loginId = (String) StpUtil.getLoginIdDefaultNull();      //TODO 前端如何传来请求头token？如果是在浏览器中测试则不用考虑
            log.info("登录信息：" + loginId);
            if (StrUtil.isEmpty(loginId)) {
                //如果没有登录则作登录操作
                //拿到uid之后做登录逻辑，判断该微博是否绑定了账号，没有的话无法使用微博登录
                UserInfoEntity socialUid = authByPasswdService.getOne(new QueryWrapper<UserInfoEntity>().eq("socialUid", socialUser.getUid()));
                if (socialUid == null) {
                    return R.error(AuthEnum.AUTH_ENUM_WEIBO_UNKNOWN.getCode(), AuthEnum.AUTH_ENUM_WEIBO_UNKNOWN.getMsg());
                }
                StpUtil.login(socialUid.getPhone());
                SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
                log.info("用户：{} 使用微博登录成功", socialUid.getPhone());
                return R.ok().put("data", tokenInfo);
            } else {
                //如果已经登录则为绑定微博操作,如果以前绑定了，则为修改为最新信息操作
                //需要查询到用户的微博号的详细内容-保存名称、描述
                try {
                    Map<String, String> mapInfo = new HashMap<>();
                    mapInfo.put("access_token", socialUser.getAccess_token());
                    mapInfo.put("uid", socialUser.getUid());
                    HttpResponse resp = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get", new HashMap<>(), mapInfo);
                    if (resp.getStatusLine().getStatusCode() == 200) {
                        String info = EntityUtils.toString(resp.getEntity());
                        JSONObject jsonObject = JSON.parseObject(info);
                        String name = jsonObject.getString("name");
                        String description = jsonObject.getString("description");
                        LambdaUpdateWrapper<UserInfoEntity> lambda = new LambdaUpdateWrapper<>();
                        lambda.set(UserInfoEntity::getSocialUid, socialUser.getUid())
                                .set(UserInfoEntity::getWeibo, name)
                                .set(UserInfoEntity::getWeiboDes, description)
                                .eq(UserInfoEntity::getPhone, loginId);
                        authByPasswdService.update(lambda);
                    } else {
                        throw new RRException("查询参数异常");
                    }
                } catch (Exception e) {
                    log.info("用户绑定微博异常！");
                    return R.error(AuthEnum.AUTH_ENUM_WEIBO_BIND.getCode(), AuthEnum.AUTH_ENUM_WEIBO_BIND.getMsg());
                }
                return R.ok();
            }
        } else {
            log.info("调用微博接口获取uid异常:{}", response);
            return R.error(AuthEnum.AUTH_ENUM_WEIBO_FAIL.getCode(), AuthEnum.AUTH_ENUM_WEIBO_FAIL.getMsg());
        }
    }

    @ApiOperation(httpMethod = "GET", value = "微博登录取消回调页")
    @GetMapping("/oauth2.0/weibo/cancel")
    public R weiboCancel() {
        return R.ok("取消登录成功");
    }

    /**
     * 邮箱登录
     *
     * @param email 邮箱号
     * @param code  邮箱验证码
     * @return R
     */
    @ApiOperation(httpMethod = "GET", value = "邮箱登录或绑定")
    @GetMapping("/email/handle")
    public R email(@RequestParam("email") String email, @RequestParam("code") String code) {
        boolean flag = PatternUtil.isEmail(email);
        if (!flag) {
            return R.error(AuthEnum.AUTH_ENUM_EMAIL_ILLEGAL.getCode(), AuthEnum.AUTH_ENUM_EMAIL_ILLEGAL.getMsg());
        }
        String loginId = (String) StpUtil.getLoginIdDefaultNull();
        if (StrUtil.isEmpty(loginId)) {
            //尚未登录时调用则为登录请求
            String codeRedis = stringRedisTemplate.opsForValue().get(AuthConstant.emailPre + email);
            if (StrUtil.isEmpty(codeRedis)) {
                return R.error(AuthEnum.AUTH_ENUM_EMAIL_ERROR.getCode(), AuthEnum.AUTH_ENUM_EMAIL_ERROR.getMsg());
            }
            if (!codeRedis.equals(code)) {
                return R.error(AuthEnum.AUTH_ENUM_EMAIL_CODE.getCode(), AuthEnum.AUTH_ENUM_EMAIL_CODE.getMsg());
            }
            UserInfoEntity userInfo = authByPasswdService.getOne(new QueryWrapper<UserInfoEntity>().eq("email", email));
            if (userInfo == null) {
                return R.error(AuthEnum.AUTH_ENUM_EMAIL_UNKNOWN.getCode(), AuthEnum.AUTH_ENUM_EMAIL_UNKNOWN.getMsg());
            }
            StpUtil.login(userInfo.getPhone());
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            log.info("用户：{} 使用邮箱登录成功", userInfo.getPhone());
            return R.ok().put("data", tokenInfo);
        }
        //已登录则为账号绑定邮箱或修改邮箱请求
        String codeRedis = stringRedisTemplate.opsForValue().get(AuthConstant.emailPre + email);
        if (StrUtil.isEmpty(codeRedis)) {
            return R.error(AuthEnum.AUTH_ENUM_EMAIL_ERROR.getCode(), AuthEnum.AUTH_ENUM_EMAIL_ERROR.getMsg());
        }
        if (codeRedis.equals(code)) {
            LambdaUpdateWrapper<UserInfoEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(UserInfoEntity::getEmail, email).eq(UserInfoEntity::getPhone, loginId);
            authByPasswdService.update(updateWrapper);
        } else {
            return R.error(AuthEnum.AUTH_ENUM_EMAIL_CODE.getCode(), AuthEnum.AUTH_ENUM_EMAIL_CODE.getMsg());
        }
        return R.ok();
    }

    /**
     * gitee登录 https://gitee.com/oauth/authorize?client_id=ca10e8e91bf4902d78d82329c77713dc36489637ae9b8d628c5fd6ab4113a4e3&redirect_uri=http://messaging.cn/api/auth/other/gitee/handle&response_type=code
     *
     * @param code code值
     * @return R
     */
    @ApiOperation(httpMethod = "GET", value = "gitee登录或绑定")
    @GetMapping("/gitee/handle")
    public R gitee(@RequestParam("code") String code) {
        log.info("gitee请求登录code:" + code);
        Map<String, String> header = new HashMap<>();
        Map<String, String> query = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("client_id", client_id_gitee);
        map.put("client_secret", client_secret_gitee);
        map.put("grant_type", grant_type_gitee);
        map.put("redirect_uri", redirect_uri_gitee);
        map.put("code", code);
        //根据code换取access_token
        HttpResponse response;
        try {
            response = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", header, query, map);
        } catch (Exception e) {
            log.info("gitee接口调用异常：{}", e.getMessage());
            return R.error(AuthEnum.AUTH_ENUM_GITEE_FAIL.getCode(), AuthEnum.AUTH_ENUM_GITEE_FAIL.getMsg());
        }

        if (response.getStatusLine().getStatusCode() == 200) {
            String json;
            try {
                json = EntityUtils.toString(response.getEntity());
                System.out.println("返回值：" + json);
                JSONObject jsonObject = JSON.parseObject(json);
                String access_token = (String) jsonObject.get("access_token");
                Map<String, String> mapInfo = new HashMap<>();
                mapInfo.put("access_token", access_token);
                HttpResponse resp = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", new HashMap<>(), mapInfo);
                if (resp.getStatusLine().getStatusCode() == 200) {
                    String info = EntityUtils.toString(resp.getEntity());
                    System.out.println("gitee用户信息:" + info);
                    JSONObject parseObject = JSON.parseObject(info);
                    String id = String.valueOf(parseObject.get("id"));
                    String name = (String) parseObject.get("name");
                    String bio = (String) parseObject.get("bio");
                    String html_url = (String) parseObject.get("html_url");

                    String loginId = (String) StpUtil.getLoginIdDefaultNull();      //TODO 前端如何传来请求头token？如果是在浏览器中测试则不用考虑
                    log.info("登录信息：" + loginId);
                    if (StrUtil.isEmpty(loginId)) {
                        //如果没有登录则作登录操作
                        //拿到id之后做登录逻辑，判断该gitee是否绑定了账号，没有的话无法使用gitee登录
                        UserInfoEntity giteeInfo = authByPasswdService.getOne(new QueryWrapper<UserInfoEntity>().eq("giteeId", id));
                        if (giteeInfo == null) {
                            return R.error(AuthEnum.AUTH_ENUM_GITEE_UNKNOWN.getCode(), AuthEnum.AUTH_ENUM_GITEE_UNKNOWN.getMsg());
                        }
                        StpUtil.login(giteeInfo.getPhone());
                        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
                        log.info("用户：{} 使用gitee登录成功", giteeInfo.getPhone());
                        return R.ok().put("data", tokenInfo);
                    } else {
                        //登录之后则为绑定或修改操作
                        LambdaUpdateWrapper<UserInfoEntity> lambda = new LambdaUpdateWrapper<>();
                        lambda.set(UserInfoEntity::getGiteeId, id)
                                .set(UserInfoEntity::getGiteeName, name)
                                .set(UserInfoEntity::getGiteeBio, bio)
                                .set(UserInfoEntity::getGiteeUrl, html_url)
                                .eq(UserInfoEntity::getPhone, loginId);
                        authByPasswdService.update(lambda);
                        log.info("账号{}绑定或修改gitee成功", loginId);
                        return R.ok();
                    }
                } else {
                    log.info("调用gitee接口获取用户信息异常:{}", resp);
                    return R.error(AuthEnum.AUTH_ENUM_GITEE_FAIL.getCode(), AuthEnum.AUTH_ENUM_GITEE_FAIL.getMsg());
                }
            } catch (Exception e) {
                log.info("异常：{}", e.getMessage());
                return R.error();
            }
        } else {
            log.info("调用gitee接口获取token异常:{}", response);
            return R.error(AuthEnum.AUTH_ENUM_GITEE_FAIL.getCode(), AuthEnum.AUTH_ENUM_GITEE_FAIL.getMsg());
        }
    }

    /**
     * github登录 https://github.com/login/oauth/authorize?client_id=a4227253bf33ae89544f&redirect_uri=http://messaging.cn/api/auth/other/github/handle
     *
     * @param code code
     * @return R
     */
    @ApiOperation(httpMethod = "GET", value = "github登录或绑定")
    @GetMapping("/github/handle")
    public R github(@RequestParam("code") String code) {
        log.info("github请求登录code:" + code);
        Map<String, String> header = new HashMap<>();
        Map<String, String> query = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("client_id", client_id_github);
        map.put("client_secret", client_secret_github);
        map.put("redirect_uri", redirect_uri_github);
        map.put("code", code);
        //根据code换取access_token
        HttpResponse response;
        try {
            response = HttpUtils.doPost("https://github.com", "/login/oauth/access_token", "post", header, query, map);
        } catch (Exception e) {
            log.info("github接口调用异常：{}", e.getMessage());
            return R.error(AuthEnum.AUTH_ENUM_GITHUB_FAIL.getCode(), AuthEnum.AUTH_ENUM_GITHUB_FAIL.getMsg());
        }
        System.out.println("响应：" + response);
        if (response.getStatusLine().getStatusCode() == 200) {
            String respToken;
            try {
                respToken = EntityUtils.toString(response.getEntity());
                System.out.println("返回值：" + respToken);      //access_token=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&scope=&token_type=bearer
                int first = respToken.indexOf("=");
                int last = respToken.indexOf("&");
                String token = respToken.substring(first + 1, last);
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("Authorization", "token " + token);
                HttpResponse resp = HttpUtils.doGet("https://api.github.com", "/user", "get", headerMap, new HashMap<>());
                if (resp.getStatusLine().getStatusCode() == 200) {
                    String info = EntityUtils.toString(resp.getEntity());
                    System.out.println("github用户信息:" + info);
                    JSONObject parseObject = JSON.parseObject(info);
                    String id = String.valueOf(parseObject.get("id"));
                    String name = (String) parseObject.get("login");
                    String bio = (String) parseObject.get("bio");
                    String html_url = (String) parseObject.get("html_url");

                    String loginId = (String) StpUtil.getLoginIdDefaultNull();      //TODO 前端如何传来请求头token？如果是在浏览器中测试则不用考虑
                    log.info("登录信息：" + loginId);
                    if (StrUtil.isEmpty(loginId)) {
                        //如果没有登录则作登录操作
                        //拿到id之后做登录逻辑，判断该github是否绑定了账号，没有的话无法使用github登录
                        UserInfoEntity giteeInfo = authByPasswdService.getOne(new QueryWrapper<UserInfoEntity>().eq("githubId", id));
                        if (giteeInfo == null) {
                            return R.error(AuthEnum.AUTH_ENUM_GITHUB_UNKNOWN.getCode(), AuthEnum.AUTH_ENUM_GITHUB_UNKNOWN.getMsg());
                        }
                        StpUtil.login(giteeInfo.getPhone());
                        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
                        log.info("用户：{} 使用github登录成功", giteeInfo.getPhone());
                        return R.ok().put("data", tokenInfo);
                    } else {
                        //登录之后则为绑定或修改操作
                        LambdaUpdateWrapper<UserInfoEntity> lambda = new LambdaUpdateWrapper<>();
                        lambda.set(UserInfoEntity::getGithubId, id)
                                .set(UserInfoEntity::getGithubName, name)
                                .set(UserInfoEntity::getGithubBio, bio)
                                .set(UserInfoEntity::getGithubUrl, html_url)
                                .eq(UserInfoEntity::getPhone, loginId);
                        authByPasswdService.update(lambda);
                        log.info("账号{}绑定或修改github成功", loginId);
                        return R.ok();
                    }
                } else {
                    log.info("调用github接口获取用户信息异常:{}", resp);
                    return R.error(AuthEnum.AUTH_ENUM_GITHUB_FAIL.getCode(), AuthEnum.AUTH_ENUM_GITHUB_FAIL.getMsg());
                }
            } catch (Exception e) {
                log.info("异常：{}", e.getMessage());
                return R.error();
            }
        } else {
            log.info("调用github接口获取token异常:{}", response);
            return R.error(AuthEnum.AUTH_ENUM_GITHUB_FAIL.getCode(), AuthEnum.AUTH_ENUM_GITHUB_FAIL.getMsg());
        }
    }

    @ApiOperation(httpMethod = "GET", value = "qq登录或绑定")
    @GetMapping("/qq/handle")
    public R qq(@RequestParam("code") String code) {
        log.info("gitee请求登录code:" + code);
        return R.ok();
    }
}
