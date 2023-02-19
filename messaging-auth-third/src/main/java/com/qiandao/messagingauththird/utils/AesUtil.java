package com.qiandao.messagingauththird.utils;

import cn.hutool.crypto.symmetric.AES;
import com.qiandao.messagingcommon.utils.RRException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AesUtil {

    @Value("${aes.secretKey}")
    private String secretKey;

    public String getAesCiphertext(String username, String passwd) {
        String salt = getSalt(username, passwd);
        AES aes = new AES("CBC", "PKCS7Padding", secretKey.getBytes(), salt.getBytes());
        return aes.encryptHex(passwd);     //加密为16进制字符串
    }

    public String getAesPlaintext(String username, String passwd, String aesCiphertext) {
        String salt = getSalt(username, passwd);
        AES aes = new AES("CBC", "PKCS7Padding", secretKey.getBytes(), salt.getBytes());
        return aes.decryptStr(aesCiphertext);     //解密
    }

    /**
     * 生成盐值
     * @param username 用户名
     * @param passwd 密码
     * @return 返回String类型16位固定盐值
     */
    public static String getSalt(String username, String passwd) {
        String salt;
        if (passwd.length() >= 16) {
            salt = passwd.substring(0, 16);
        } else {
            int len = 16 - passwd.length();
            String substring = username.substring(0, len);
            salt = passwd + substring;
        }
        if (salt.length() < 16) {
            throw new RRException("账号密码不符合规范");
        }
        return salt;
    }
}
