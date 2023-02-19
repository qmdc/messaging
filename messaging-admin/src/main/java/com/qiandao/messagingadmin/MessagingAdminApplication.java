package com.qiandao.messagingadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.qiandao.messagingadmin.dao")
public class MessagingAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingAdminApplication.class, args);
    }

}
