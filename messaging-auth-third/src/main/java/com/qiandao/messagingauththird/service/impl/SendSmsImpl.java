package com.qiandao.messagingauththird.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.qiandao.messagingauththird.service.SendSms;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SendSmsImpl implements SendSms {

    @Value("${tencent-sms.secretId}")
    private String secretId;

    @Value("${tencent-sms.secretKey}")
    private String secretKey;

    @Value("${tencent-sms.sdkAppId}")
    private String sdkAppId;

    @Value("${tencent-sms.signName}")
    private String signName;

    @Value("${tencent-sms.register_templateId}")
    private String register_templateId;

    @Value("${tencent-sms.retrieve_templateId}")
    private String retrieve_templateId;

    @Value("${tencent-sms.login_templateId}")
    private String login_templateId;

    public boolean send(String[] phones, String code, String templateId) {
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(secretId, secretKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            SendSmsRequest req = new SendSmsRequest();
            req.setPhoneNumberSet(phones);

            req.setSmsSdkAppId(sdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            String[] templateParamSet1 = null;
            if (templateId.equals(register_templateId)) {
                //注册短信
                templateParamSet1 = new String[]{code, "5"};
            } else if (templateId.equals(retrieve_templateId)) {
                //找回密码
                templateParamSet1 = new String[]{code,"3"};
            } else if (templateId.equals(login_templateId)) {
                //登录短信
                templateParamSet1 = new String[]{code,"2"};
            }
            req.setTemplateParamSet(templateParamSet1);
            // 返回的resp是一个SendSmsResponse的实例，与请求对象对应
            SendSmsResponse resp = client.SendSms(req);
            // 输出json格式的字符串回包
            log.info(SendSmsResponse.toJsonString(resp));

            String jsonString = SendSmsResponse.toJsonString(resp);
            Map<String, Object> map = JSON.parseObject(jsonString);
            JSONArray sendStatusSet = (JSONArray) map.get("SendStatusSet");
            Map hashMap = JSON.parseObject(sendStatusSet.getString(0), HashMap.class);
            String respCode = (String) hashMap.get("Code");

            return respCode.equals("Ok");
        } catch (TencentCloudSDKException e) {
            log.info(e.toString());
            return false;
        }
    }
}
