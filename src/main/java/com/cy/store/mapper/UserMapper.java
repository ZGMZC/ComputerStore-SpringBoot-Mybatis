package com.cy.store.mapper;

import com.cy.store.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

//用户模块的持久层接口
@Repository
public interface UserMapper {
    /**
     * 插入用户的数据
     * @param user  用户的数据
     * @return 受影响的行数，可以根据返回值判断是否成功
     */
    Integer insert(User user);

    /**
     * 根据用户名来查询用户的数据
     * @param username 用户名
     * @return 如果找到对应的用户则返回这个用户的数据，如果没有找到则返回null
     */
    User findByUsername(String username);
    /**
     * 更新密码
     * @param uid   用户id
     * @param password  新密码
     * @param modifiedUser  修改者
     * @param modifiedTime  修改时间
     * @return
     */
    Integer updatePasswordByUid(Integer uid, String password, String modifiedUser, Date modifiedTime);

    /**
     * 根据uid查询数据
     * @param uid
     * @return
     */
    User findByUid(Integer uid);

    /**
     * 更新用户信息
     * @param user  用户数据
     * @return  返回值为受影响的行数
     */
    Integer updateInfoByUid(User user);

    /**
     * @param("SQl映射文件中#{}占位符的变量名")：当SQL语句占位符和映射接口方法参数名不一致时，将某个参数强行注入到占位符中。
     * 修改用户头像，根据uid值
     * @param uid
     * @param avatar
     * @param modifiedUser
     * @param modifiedTime
     * @return
     */
    Integer updateAvatarByUid(@Param("uid") Integer uid, String avatar, String modifiedUser, Date modifiedTime);
}
