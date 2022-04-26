package com.cy.store.mapper;

import com.cy.store.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@SpringBootTest  //表示当前的类是测试类
@RunWith(SpringRunner.class)  //启动类,传递SpringRunner实例类型
public class UserMapperTests {

    //idea 有检测功能，接口不能直接创建bean的（动态代理来实现）
    @Autowired
    private UserMapper userMapper;

    /**
     * 1. 必须被Test修饰
     * 2. 返回值 void
     * 3. 参数不能指定任意类型
     * 4. 访问修饰符 public
     */
    @Test
    public void insert(){
        User user=new User();
        user.setUsername("tim");
        user.setPassword("123");
        Integer rows = userMapper.insert(user);
        System.out.println(rows);
    }
    @Test
    public void findByUsername(){
        User user = userMapper.findByUsername("tim");
        System.out.println(user);
    }
    @Test
    public void updatePasswordByUid(){
        userMapper.updatePasswordByUid(1,"321","管理员",new Date());
    }
    @Test
    public void findByUid(){
        System.out.println(userMapper.findByUid(1));
    }
    @Test
    public void updateInfoByUid(){
        User user=new User();
        user.setUid(5);
        user.setPhone("15456135711");
        user.setEmail("45645@45.com");
        userMapper.updateInfoByUid(user);
    }
    @Test
    public void  updateAvatarByUid(){
        userMapper.updateAvatarByUid(5,"dsada","管理员",new Date());
    }


}
