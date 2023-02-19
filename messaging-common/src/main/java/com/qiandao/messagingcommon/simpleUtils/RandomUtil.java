package com.qiandao.messagingcommon.simpleUtils;

import java.util.Random;
import java.util.UUID;

public class RandomUtil {

    /**
     *
     * @param n 位数
     * @return 返回uuid的前n位
     */
    public static String getUUID(int n) {
        String uuid = UUID.randomUUID().toString();
        String replace = uuid.replace("-", "");
        return replace.substring(0,n);
    }

    /**
     *
     * @return 返回6位短信验证码
     */
    public static String getCode() {
        int intCode = new Random().nextInt(111111, Integer.MAX_VALUE);
        return String.valueOf(intCode).substring(0, 6);
    }



}
