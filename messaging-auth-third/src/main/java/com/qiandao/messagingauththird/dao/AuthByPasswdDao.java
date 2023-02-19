package com.qiandao.messagingauththird.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandao.messagingauththird.entity.UserInfoEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthByPasswdDao extends BaseMapper<UserInfoEntity> {

}
