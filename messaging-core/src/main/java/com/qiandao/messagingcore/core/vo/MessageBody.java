package com.qiandao.messagingcore.core.vo;

import lombok.Data;

@Data
public class MessageBody {

    /**
     * 发送人姓名
     */
    private String fromName;

    /**
     * 接收人姓名
     */
    private String toName;

    /**
     * 消息内容
     */
    private String content;

    /**
     *
     * 发送时间
     */
//    @JSONField(format="yyyy-MM-dd HH:mm:ss")
    private String sendTime;
}
