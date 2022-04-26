package com.cy.store.service;

import com.cy.store.entity.Address;
import com.cy.store.entity.District;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest  //表示当前的类是测试类
@RunWith(SpringRunner.class)  //启动类,传递SpringRunner实例类型
public class DistrictServiceTests {

    //idea 有检测功能，接口不能直接创建bean的（动态代理来实现）
    @Autowired
    private IDistrictService districtService;

    /**
     * 1. 必须被Test修饰
     * 2. 返回值 void
     * 3. 参数不能指定任意类型
     * 4. 访问修饰符 public
     */
    @Test
    public void getByParent(){
        List<District> list = districtService.getByParent("86");
        for(District d:list)
            System.out.println(d);
    }
}
