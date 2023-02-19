package com.qiandao.messagingcore.core.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@Configuration
public class WebSocketConfig implements ServletContextInitializer {

    /**
     * 这个bean的注册,用于扫描带有@ServerEndpoint的注解成为websocket,如果你使用外置的tomcat就不需要该配置文件
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

    }

    /**
     * websocket 配置信息
     *
     * @return
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer()
    {
        ServletServerContainerFactoryBean bean = new ServletServerContainerFactoryBean();
        //文本缓冲区大小
        bean.setMaxTextMessageBufferSize(8192);
        //字节缓冲区大小
        bean.setMaxBinaryMessageBufferSize(8192);
        return bean;
    }

}
