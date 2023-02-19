package com.qiandao.messagingauththird;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

//@EnableAspectJAutoProxy(exposeProxy=true)
@EnableDiscoveryClient
@MapperScan("com.qiandao.messagingauththird.dao")
@SpringBootApplication
public class MessagingAuthThirdApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingAuthThirdApplication.class, args);
    }

}
