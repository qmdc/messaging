package com.qiandao.messagingadmin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiandao.messagingadmin.dao.UserMapper;
import com.qiandao.messagingadmin.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MessagingAdminApplicationTests {

    @Autowired
    private UserMapper userMapper;

    /**
     * 写入数据的测试
     */
    @Test
    public void testInsert(){

        User user = new User();
        user.setUname("张三丰");
        userMapper.insert(user);
    }

    @Test
    void read() {
        for (int i = 0; i <= 5; i++) {
            List<User> id = userMapper.selectList(new QueryWrapper<User>().eq("id", 3));
            System.out.println("=======>>>>>>>>");
        }
    }

}
