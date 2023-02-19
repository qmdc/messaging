package com.qiandao.messagingcore;

import com.qiandao.messagingcore.core.constant.EnumSingleMap;
import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.websocket.Session;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class MessagingCoreApplicationTests {

    @Test   //测试之前先把'serverEndpointExporter' WebSocketConfig类注释掉
    void contextLoads() {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CopyOnWriteArraySet<Object> objects = new CopyOnWriteArraySet<>();
        // HashSet<Object> objects = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            pool.execute(()->{
                for (int j = 0; j < 10; j++) {
                    ConcurrentHashMap<String, Session> fromMap = EnumSingleMap.ConcurrentHashMap.fromMap;
                    objects.add(fromMap.hashCode());
                }
            });
        }
        System.out.println(objects.size());     //结果为1则正常
    }

}
