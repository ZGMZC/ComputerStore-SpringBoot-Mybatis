package com.cy.store.mapper;

import com.cy.store.entity.Address;
import com.cy.store.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@SpringBootTest  //表示当前的类是测试类
@RunWith(SpringRunner.class)  //启动类,传递SpringRunner实例类型
public class AddressMapperTests {

    //idea 有检测功能，接口不能直接创建bean的（动态代理来实现）
    @Autowired
    private AddressMapper addressMapper;

    /**
     * 1. 必须被Test修饰
     * 2. 返回值 void
     * 3. 参数不能指定任意类型
     * 4. 访问修饰符 public
     */
    @Test
    public void insert(){
        Address address=new Address();
        address.setUid(5);
        address.setPhone("12345678825");
        address.setName("女朋友");
        addressMapper.insert(address);
    }
    @Test
    public void countByUid(){
        Integer count=addressMapper.countByUid(5);
        System.out.println(count);
    }
    @Test
    public void findByUid(){
        List<Address> list = addressMapper.findByUid(5);
        System.out.println(list);
    }
    @Test
    public void updateNonDefault(){
        addressMapper.updateNonDefault(5);
    }
    @Test
    public void updateDefaultByAid(){
        addressMapper.updateDefaultByAid(5,"管理员",new Date());
    };
    @Test
    public void findByAid(){
        System.out.println(addressMapper.findByAid(5));
    }
    @Test
    public void deleteByAid(){
        addressMapper.deleteByAid(6);
    }
    @Test
    public void findLastModified(){
        System.out.println(addressMapper.findLastModified(6));
    }
}
