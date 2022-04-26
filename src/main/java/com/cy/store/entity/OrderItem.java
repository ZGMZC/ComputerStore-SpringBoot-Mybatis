package com.cy.store.entity;
import lombok.Data;

import java.io.Serializable;

@Data
/** 订单中的商品数据 */
public class OrderItem extends BaseEntity implements Serializable {
    private Integer id;
    private Integer oid;
    private Integer pid;
    private String title;
    private String image;
    private Long price;
    private Integer num;

    // Generate: Getter and Setter、Generate hashCode() and equals()、toString()
}