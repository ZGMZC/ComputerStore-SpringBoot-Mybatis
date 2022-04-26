package com.cy.store.service;

import com.cy.store.entity.User;
import com.cy.store.mapper.UserMapper;
import com.cy.store.service.ex.ServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest  //表示当前的类是测试类
@RunWith(SpringRunner.class)  //启动类,传递SpringRunner实例类型
public class UserServiceTests {

    //idea 有检测功能，接口不能直接创建bean的（动态代理来实现）
    @Autowired
    private IUserService userService;

    /**
     * 1. 必须被Test修饰
     * 2. 返回值 void
     * 3. 参数不能指定任意类型
     * 4. 访问修饰符 public
     */
    @Test
    public void reg(){
        try {
            User user=new User();
            user.setUsername("yuanxin2");
            user.setPassword("123");
            userService.reg(user);
            System.out.println("ok");
        } catch (ServiceException e) {
            System.out.println(e.getClass().getSimpleName());
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void login(){
        User user = userService.login("tom", "123");
        System.out.println(user);
    }
    @Test
    public void changePassword(){
        userService.changePassword(4,"张三","123","321");
    }

    @Test
    public void getByUid(){
        System.out.println(userService.getByUid(5));
    }
    @Test
    public void changeInfo(){
        User user=new User();
        user.setPhone("13245679853");
        user.setEmail("478631@qq.com");
        user.setGender(0);
        userService.changeInfo(3,"管理员",user);
    }
    @Test
    public void changeAvatar(){
        userService.changeAvatar(5,"fdsf","管理员");
    }
}
