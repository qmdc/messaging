package com.qiandao.messagingadmin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiandao.messagingadmin.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
