package com.cy.store.service.impl;

import com.cy.store.entity.User;
import com.cy.store.mapper.UserMapper;
import com.cy.store.service.IUserService;
import com.cy.store.service.ex.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.UUID;

/**
 * 用户模块业务层的实现类
 */
@Service  //将当前类的对象交给Spring管理，自动创建对象及对象维护
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void reg(User user) {
        //通过user参数获取username
        //调用查找 判断是否被注册过
        String username = user.getUsername();
        User result = userMapper.findByUsername(username);
        //判断结果集是否为空
        if(result!=null){
            throw new UsernameDuplicatedException("用户名被占用");
        }

        //密码加密处理，md5算法的形式
        //串+password+串————md5算法进行加密 ，连续加密三次
        //盐值+password+盐值————盐值，随机的字符串
        String oldpassword=user.getPassword();
        //获取盐值（随机生成盐值）
        String salt = UUID.randomUUID().toString().toUpperCase();
        //将密码和盐值作为整体进行加密处理
        String md5Password = getMD5Password(oldpassword, salt);
        //将加密后的密码 重新设置
        user.setPassword(md5Password);

        /* 创建者、创建时间、修改者、修改时间、is_delete等信息*/
        //补全数据
        user.setIsDelete(0);
        user.setCreatedUser(username);
        user.setModifiedUser(username);
        Date date=new Date();
        user.setCreatedTime(date);
        user.setModifiedTime(date);
        user.setSalt(salt);

        //执行注册功能
        Integer rows = userMapper.insert(user);
        if(rows!=1){
            throw new InsertException("在用户注册过程中产生了未知的异常");
        }


    }

    @Override
    public User login(String username, String password) {
        //根据用户名称查询用户数据是否存在
        User result = userMapper.findByUsername(username);
        if(result==null)
            throw new UserNotFoundException("用户不存在");
        //检测用户的密码是否匹配
        //1.先获取到数据库加密后的密码
        String oldPassword=result.getPassword();
        //2.和用户传递过来的密码进行比较
        //2.1获取盐值
        String salt=result.getSalt();
        //2.1将用户密码按照相同的md5算法的规则进行加密
        String newMd5Password=getMD5Password(password,salt);
        if(!newMd5Password.equals(oldPassword)){
            throw new PasswordNotMatchException("用户密码错误");
        }

        //判断is_delete字段的值是否为1 ，表示是否被删除
        if(result.getIsDelete()==1){
            throw new UserNotFoundException("用户不存在");
        }

        User user = new User();
        user.setUid(result.getUid());
        user.setUsername(result.getUsername());
        user.setAvatar(result.getAvatar());

        return user;
    }

    @Override
    public User getByUid(Integer uid) {
        User result = userMapper.findByUid(uid);
        if(result==null||result.getIsDelete()==1) throw  new UserNotFoundException("用户数据不存在");
        User user=new User();
        user.setUsername(result.getUsername());
        user.setPhone(result.getPhone());
        user.setEmail(result.getEmail());
        user.setGender(result.getGender());
        return user;
    }

    @Override
    public void changeInfo(Integer uid, String username, User user) {
        User result=userMapper.findByUid(uid);
        if(result==null||result.getIsDelete()==1) throw  new UserNotFoundException("用户数据不存在");
        user.setUid(uid);
        user.setModifiedUser(username);
        user.setModifiedTime(new Date());
        Integer rows = userMapper.updateInfoByUid(user);
        if(rows!=1) throw new UpdateException("更新数据时产生的未知异常");
    }

    @Override
    public void changePassword(Integer uid, String username, String oldPassword, String newPassword) {
        User result = userMapper.findByUid(uid);
        if(result==null||result.getIsDelete()==1)
            throw new UserNotFoundException("用户数据不存在");
        String oldMd5Password=getMD5Password(oldPassword,result.getSalt());
        if(!result.getPassword().equals(oldMd5Password))
            throw new PasswordNotMatchException("密码错误");
        String newMd5Password = getMD5Password(newPassword, result.getSalt());
        Integer rows = userMapper.updatePasswordByUid(uid, newMd5Password, username, new Date());
        if(rows!=1)
            throw new UpdateException("更新数据时产生的未知异常");
    }

    @Override
    public void changeAvatar(Integer uid, String avatar, String username) {
        //查询当前的用户数据是否存在
        User result = userMapper.findByUid(uid);
        if(result==null||result.getIsDelete()==1)
            throw new UserNotFoundException("用户数据不存在");
        Integer rows = userMapper.updateAvatarByUid(uid, avatar, username, new Date());
        if(rows!=1)
            throw new UpdateException("更新用户头像时产生的未知异常");
    }
    /**
     * 定义md5算法加密处理
     */
    private String getMD5Password(String password,String salt){
        for (int i=0;i<3;i++)
        password= DigestUtils.md5DigestAsHex((salt+password+salt).getBytes()).toUpperCase(); //md5加密算法的调用
        return password;
    }


}
