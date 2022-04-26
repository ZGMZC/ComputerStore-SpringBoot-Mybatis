package com.cy.store.service;


import com.cy.store.entity.User;

import java.util.Date;

/**
 * 用户模块业务层接口
 */
public interface IUserService {
    /**
     * 用户注册
     * @param user
     */
    void reg(User user);

    /**
     * 用户登录
     * @param username 用户名
     * @param password 用户密码
     * @return
     */
    User login(String username,String password);

    /**
     * 修改密码
     * @param uid
     * @param username
     * @param oldPassword
     * @param newPassword
     */
    void changePassword(Integer uid,String username,String oldPassword,String newPassword);

    /**
     * 根据用户id查询用户的数据
     * @param uid
     * @return
     */
    User getByUid(Integer uid);

    /**
     * 根据用户id修改信息
     * @param uid
     * @param username
     * @param user
     */
    void changeInfo(Integer uid,String username,User user);

    /**
     * 修改用户头像
     * @param uid
     * @param avatar    用户头像路径
     * @param username
     */
    void changeAvatar(Integer uid, String avatar, String username);
}
