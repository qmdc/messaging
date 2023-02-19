package com.qiandao.messagingcore.core.pubsub;

import com.qiandao.messagingcore.core.constant.RedisSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisChannelContainer {

    /**
     * single消息适配器
     *
     * @return MessageListenerAdapter
     */
    @Bean
    MessageListenerAdapter single_Adapter(RedisSubscribeMsg redisSubscribeMsg) {  //与channel绑定的适配器
        return new MessageListenerAdapter(redisSubscribeMsg, "singleMessage"); //收到消息时执行RedisSingleSubscribe类中的onMessage方法
    }

    /**
     * room消息适配器
     *
     * @return MessageListenerAdapter
     */
    @Bean
    MessageListenerAdapter room_Adapter(RedisSubscribeMsg redisSubscribeMsg) {  //与channel绑定的适配器
        return new MessageListenerAdapter(redisSubscribeMsg, "roomMessage"); //收到消息时执行RedisSingleSubscribe类中的roomMessage方法
    }

    /**
     * allUser消息适配器
     *
     * @return MessageListenerAdapter
     */
    @Bean
    MessageListenerAdapter allUser_Adapter(RedisSubscribeMsg redisSubscribeMsg) {  //与channel绑定的适配器
        return new MessageListenerAdapter(redisSubscribeMsg, "allUserMessage"); //收到消息时执行RedisSingleSubscribe类中的allUserMessage方法
    }

    /**
     * 定义消息监听者容器
     * @param connectionFactory 连接工厂
     * @param single_Adapter 消息处理器
     * @return RedisMessageListenerContainer
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter single_Adapter,
                                            MessageListenerAdapter room_Adapter,
                                            MessageListenerAdapter allUser_Adapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(single_Adapter, new PatternTopic(RedisSocket.REDIS_CHANNEL_SINGLE)); //将订阅者Single_Adapter与channel_single频道绑定
        container.addMessageListener(room_Adapter, new PatternTopic(RedisSocket.REDIS_CHANNEL_ROOM)); //将订阅者Room_Adapter与channel_single频道绑定
        container.addMessageListener(allUser_Adapter, new PatternTopic(RedisSocket.REDIS_CHANNEL_ALL_USER)); //将订阅者allUser_Adapter与channel_allUser频道绑定
        return container;
    }

}
