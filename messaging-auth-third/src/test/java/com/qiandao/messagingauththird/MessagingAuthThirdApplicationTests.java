package com.qiandao.messagingauththird;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@SpringBootTest
class MessagingAuthThirdApplicationTests {

    @Test
    void contextLoads() {
    }


    @Test
    void passwd() {
        String content = "test中文";

        // 随机生成密钥
        String value = SymmetricAlgorithm.AES.getValue();
        System.out.println(value);
        byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();

        // 构建
        AES aes = SecureUtil.aes(key);
//        System.out.println(aes);
//
//        // 加密
//        byte[] encrypt = aes.encrypt(content);
//        System.out.println(Arrays.toString(encrypt));
//        // 解密
//        byte[] decrypt = aes.decrypt(encrypt);
//        System.out.println(Arrays.toString(decrypt));

        // 加密为16进制表示
        String encryptHex = aes.encryptHex(content);
        System.out.println(encryptHex);
        // 解密为字符串
        String decryptStr = aes.decryptStr(encryptHex, CharsetUtil.CHARSET_UTF_8);
        System.out.println(decryptStr);
    }

    @Test
    void aes() {
        String content = "我是谁？";
        AES aes = new AES("CBC", "PKCS7Padding",
                // 密钥，可以自定义
                "0123456789ABHAEQ".getBytes(),
                // iv加盐，按照实际需求添加
                "DYgjCEIMVrj2W9xN".getBytes());

        // 加密为16进制表示
        String encryptHex = aes.encryptHex(content);
        System.out.println(encryptHex);
        // 解密
        String decryptStr = aes.decryptStr(encryptHex);
        System.out.println(decryptStr);
    }

    @Test
    void testAES() {
        AES aes = new AES("CBC", "PKCS7Padding",
                // 密钥，可以自定义
                "0123456789ABHAEQ".getBytes(),
                // iv加盐，按照实际需求添加
                "DYgjCEIMVrj2W9xN".getBytes());
        // 解密
        String decryptStr = aes.decryptStr("4da05a7b71ce4741e49d3c649a2ea123");
        System.out.println(decryptStr);
    }

}
