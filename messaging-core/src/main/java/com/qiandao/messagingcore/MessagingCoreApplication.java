package com.qiandao.messagingcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.qiandao.messagingcore.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class MessagingCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingCoreApplication.class, args);
    }

}
