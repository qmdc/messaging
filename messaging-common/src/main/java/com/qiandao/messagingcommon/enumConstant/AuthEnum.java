package com.qiandao.messagingcommon.enumConstant;

public enum AuthEnum {

    AUTH_ENUM_REGISTER_FAIL("A0100","用户注册错误"),
    AUTH_ENUM_NAME_FAIL("A0201","用户账户不存在"),
    AUTH_ENUM_PASSWD_FAIL("A0120","密码校验失败"),
    AUTH_ENUM_REGISTER_EXISTENCE("A0111","用户名已存在"),
    AUTH_ENUM_PHONE_EXISTENCE("A0112","手机号已注册"),
    AUTH_ENUM_PHONE_FAIL("A0151","手机格式校验失败"),
    AUTH_ENUM_PHONE_UNREGISTERED("A0152","手机号尚未注册"),
    AUTH_ENUM_PHONE_REPEAT("A0506","用户重复请求"),
    AUTH_ENUM_PHONE_OVERTIME("A0241","用户验证码已过期"),
    AUTH_ENUM_PHONE_ERROR("A0240","用户验证码错误"),
    AUTH_ENUM_LEN_SHORT("A0121","密码长度不够"),
    AUTH_ENUM_LEN_FAIL("A0121","新密码格式错误"),
    AUTH_ENUM_ROLE_FAIL("A0220","用户身份校验失败"),
    AUTH_ENUM_PERM_FAIL("A0300","访问权限异常"),
    AUTH_ENUM_ERROR("11111","系统未知异常"),
    AUTH_ENUM_TOKEN("A0230","用户登陆已过期"),
    AUTH_ENUM_VALID("C0134","不支持的数据格式"),
    AUTH_ENUM_REDIS_OVERTIME("C0230","redis缓存服务超时"),
    AUTH_ENUM_EMAIL_ILLEGAL("O10001","不是合法邮箱地址"),
    AUTH_ENUM_EMAIL_UNKNOWN("O10002","邮箱尚未绑定账号"),
    AUTH_ENUM_EMAIL_ERROR("O10003","邮箱号异常"),
    AUTH_ENUM_EMAIL_CODE("O10004","邮箱验证码错误"),
    AUTH_ENUM_EMAIL_FAIL("O10005","邮箱发信失败"),
    AUTH_ENUM_EMAIL_REPEAT("O10005","邮箱验证码尚未失效"),

    AUTH_ENUM_WEIBO_UNKNOWN("O2001","微博尚未绑定账号"),
    AUTH_ENUM_WEIBO_FAIL("O2011","微博调用失败"),
    AUTH_ENUM_WEIBO_BIND("O2021","微博绑定失败"),

    AUTH_ENUM_GITEE_UNKNOWN("O2001","Gitee尚未绑定账号"),
    AUTH_ENUM_GITEE_FAIL("O2011","Gitee调用失败"),
    AUTH_ENUM_GITEE_BIND("O2021","Gitee绑定失败"),

    AUTH_ENUM_GITHUB_UNKNOWN("O2001","Github尚未绑定账号"),
    AUTH_ENUM_GITHUB_FAIL("O2011","Github调用失败"),
    AUTH_ENUM_GITHUB_BIND("O2021","Github绑定失败"),



    AUTH_ENUM_WECHAT_UNKNOWN("O2002","微信尚未绑定账号"),
    AUTH_ENUM_WECHAT_FAIL("O2012","微信调用失败"),
    AUTH_ENUM_WECHAT_BIND("O2022","微信绑定失败"),
    AUTH_ENUM_QQ_UNKNOWN("O2003","QQ尚未绑定账号"),
    AUTH_ENUM_QQ_FAIL("O2013","QQ调用失败"),
    AUTH_ENUM_QQ_BIND("O2023","QQ绑定失败");


    private final String  code;
    private final String msg;

    AuthEnum(String code,String msg){
        this.code = code;
        this.msg = msg;
    }

    public String getCode(){
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
