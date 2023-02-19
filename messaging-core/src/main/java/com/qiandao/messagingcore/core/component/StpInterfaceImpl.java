package com.qiandao.messagingcore.core.component;

import cn.dev33.satoken.stp.StpInterface;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.qiandao.messagingcommon.utils.R;
import com.qiandao.messagingcore.feign.AuthFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义权限验证接口扩展
 * 保证此类被SpringBoot扫描，完成Sa-Token的自定义权限验证扩展
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private AuthFeignService authFeignService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        R r = authFeignService.getPermList((String) loginId);
        Object data = r.get("data");
        String s = JSON.toJSONString(data);
        return JSON.parseObject(s, new TypeReference<>() {
        });
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String id = (String)loginId;
        R r = authFeignService.getRoleList(id);
        Object data = r.get("data");
        String s = JSON.toJSONString(data);
        return JSON.parseObject(s, new TypeReference<>() {
        });
    }

}
