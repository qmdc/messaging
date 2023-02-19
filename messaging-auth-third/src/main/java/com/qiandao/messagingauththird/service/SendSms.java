package com.qiandao.messagingauththird.service;

public interface SendSms {

    boolean send(String[] phoneNum, String code, String templateId);

}
