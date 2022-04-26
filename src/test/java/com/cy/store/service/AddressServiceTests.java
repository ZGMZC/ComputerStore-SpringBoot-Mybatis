package com.cy.store.service;

import com.cy.store.entity.Address;
import com.cy.store.entity.User;
import com.cy.store.service.ex.ServiceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest  //表示当前的类是测试类
@RunWith(SpringRunner.class)  //启动类,传递SpringRunner实例类型
public class AddressServiceTests {

    //idea 有检测功能，接口不能直接创建bean的（动态代理来实现）
    @Autowired
    private IAddressService addressService;

    /**
     * 1. 必须被Test修饰
     * 2. 返回值 void
     * 3. 参数不能指定任意类型
     * 4. 访问修饰符 public
     */
    @Test
    public void addNewAddress(){
        Address address=new Address();
        address.setPhone("12345678875");
        address.setName("男朋友");
        addressService.addNewAddress(5,"管理员",address);
    }
    @Test
    public void setDefault(){
        addressService.setDefault(5,6,"管理员");
    }
    @Test
    public void delete(){
        addressService.delete(2,5,"管理员");
    }
}
