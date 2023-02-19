package com.qiandao.messagingcommon.simpleUtils;

import java.util.regex.Matcher;

public class PatternUtil {

    /**
     * 判断是否为11为手机号
     * @param mobile 11位手机号
     * @return true/false
     */
    public static boolean isMobileNO(String mobile) {
        java.util.regex.Pattern p = java.util.regex.Pattern
                .compile("^1[345789]\\d{9}$");
        Matcher m = p.matcher(mobile);
        return m.matches();
    }

    /**
     * 判断是否为合法邮箱账号格式
     * @param email 邮箱地址
     * @return true/false
     */
    public static boolean isEmail(String email) {
        java.util.regex.Pattern p = java.util.regex.Pattern
                .compile("^\\s*\\w+(?:\\.{0,1}[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$");
        Matcher m = p.matcher(email);
        return m.matches();
    }
}
