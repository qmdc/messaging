package com.qiandao.messagingauththird.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qiandao.messagingauththird.entity.UserInfoEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface AuthByPasswdService extends IService<UserInfoEntity> {

    //查询用户权限
    List<String > getPermList(String loginId);

    //查询用户角色
    List<String > getRoleList(String loginId);

    //发送短信验证码
    String sendSmsPattern(String phone, String templateId);

    //插入用户
    boolean insertUser(UserInfoEntity userInfoEntity);

    //修改密码
    boolean userModifyPasswd(String oldPasswd, String newPasswd);

    //重设密码
    void retrievePasswd(String phone,String newPasswd);

    //根据loginId获取no
    String getNoByFromId(String fromId);

    //根据no获取用户头像
    Map<String, String> getUserImageByNo(Map<String, String> users);

    //添加用户违规记录
    int insertUserTaboo(String messages,String userId);
}
