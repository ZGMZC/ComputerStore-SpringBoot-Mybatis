package com.cy.store.entity;


import lombok.Data;
import org.springframework.stereotype.Component;

//用户的实体类
@Data
public class User extends BaseEntity{
    private Integer uid;
    private String username;
    private String password;
    private String salt;
    private String  phone;
    private String email;
    private Integer gender;
    private String avatar;
    private Integer isDelete;
}
