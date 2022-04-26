package com.cy.store.entity;

import lombok.Data;

//表示省市区的实体类
@Data
public class District extends BaseEntity{
    private Integer id;
    private String parent;
    private String code;
    private String name;
}
