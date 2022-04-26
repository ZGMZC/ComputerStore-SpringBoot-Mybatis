package com.cy.store.mapper;

import com.cy.store.entity.District;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DistrictMapper {
    /**
     * 根据父代号查询
     * @param parent
     * @return
     */
    List<District> findByParent(String parent);

    /**
     *
     * @param code
     * @return
     */
    String findNameByCode(String code);
}
