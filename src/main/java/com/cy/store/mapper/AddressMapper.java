package com.cy.store.mapper;

import com.cy.store.entity.Address;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

//收货地址持久层的接口
@Repository
public interface AddressMapper {
    /**
     * 插入用户的收货地址数据
     * @param address   收货地址
     * @return  受影响的行数
     */
    Integer insert(Address address);

    /**
     * 根据用户id统计收获地址数量
     * @param uid   用户id
     * @return  当前用户的收货地址总数
     */
    Integer countByUid(Integer uid);

    /**
     * 根据用户id查询收货地址
     * @param uid
     * @return  收货地址
     */
    List<Address> findByUid(Integer uid);

    /**
     * 根据aid查询收货地址数据
     * @param aid
     * @return  收货地址数据
     */
    Address findByAid(Integer aid);

    /**
     * 根据uid修改收货地址
     * @param uid
     * @return
     */
    Integer updateNonDefault(Integer uid);

    Integer updateDefaultByAid(@Param("aid") Integer aid, @Param("modifiedUser") String modifiedUser, @Param("modifiedTime") Date modifiedTime);

    Integer deleteByAid(Integer aid);

    /**
     * 根据uid查询最后一次修改的数据
     * @param uid
     * @return  收货地址
     */
    Address findLastModified(Integer uid);

}
