package com.cy.store.service;

import com.cy.store.entity.Product;
import org.springframework.stereotype.Service;

import java.util.List;

public interface IProductService {
    /**
     * 找出前四个热销商品
     * @return
     */
    List<Product> findHotList();

    /**
     * 根据商品id查询商品详情
     * @param id
     * @return
     */
    Product findById(Integer id);
}
