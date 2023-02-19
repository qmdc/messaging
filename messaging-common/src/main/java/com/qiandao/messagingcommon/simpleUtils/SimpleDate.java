package com.qiandao.messagingcommon.simpleUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleDate {

    /**
     *
     * @return 字符串 获取当前（yyyy-MM-dd HH:mm:ss）时间
     */
    public static String nowtime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return simpleDateFormat.format(date);
    }

    /**
     *
     * @return 字符串 获取当前（yyyy-MM-dd HH:mm:ss:SSS）精确到毫秒时间
     */
    public static String nowtimeSSS() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        return simpleDateFormat.format(System.currentTimeMillis());
    }
}
