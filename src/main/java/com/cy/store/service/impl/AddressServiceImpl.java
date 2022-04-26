package com.cy.store.service.impl;

import com.cy.store.entity.Address;
import com.cy.store.mapper.AddressMapper;
import com.cy.store.service.IAddressService;
import com.cy.store.service.IDistrictService;
import com.cy.store.service.ex.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AddressServiceImpl implements IAddressService {
    @Autowired
    private AddressMapper addressMapper;
    //添加用户收货地址的业务层依赖于DistrictService接口
    @Autowired
    private IDistrictService districtService;
    @Value("${user.address.max-count}")
    private Integer maxCount;
    @Override
    public void delete(Integer aid, Integer uid, String username) {
        Address result = addressMapper.findByAid(aid);
        if(result==null) throw new AddressNotFoundException("收货地址不存在");
        if(!result.getUid().equals(uid)) throw new AccessDeniedException("非法数据访问");
        Integer rows = addressMapper.deleteByAid(aid);
        if(rows!=1) throw new DeleteException("删除数据产生异常");
        Integer count = addressMapper.countByUid(uid);
        if(count==0) return;

        if(result.getIsDefault()==1){
            Address address = addressMapper.findLastModified(uid);
            rows=addressMapper.updateDefaultByAid(address.getAid(),username,new Date());
        }
        if(rows!=1) throw new UpdateException("更新数据时产生未知的异常");
    }
    @Override
    public void setDefault(Integer aid, Integer uid, String username) {
        Address result = addressMapper.findByAid(aid);
        if(result==null) throw new AddressNotFoundException("收货地址不存在");
        //检测当前获取到的数据归属
        if(!result.getUid().equals(uid)) throw new AccessDeniedException("非法数据访问");
        Integer rows = addressMapper.updateNonDefault(uid);
        if(rows<1) throw new UpdateException("更新数据产生异常");
        rows=addressMapper.updateDefaultByAid(aid,username,new Date());
        if(rows!=1) throw new UpdateException("更新数据产生异常");
    }

    @Override
    public void addNewAddress(Integer uid, String username, Address address) {
        //调用收货地址统计的方法
        Integer count=addressMapper.countByUid(uid);
        if(count>=maxCount){
            throw new AddressCountLimitException("用户收货地址超出上限");
        }
        //对address对象中的数据进行补全，省市区
        String provinceName = districtService.getNameByCode(address.getProvinceCode());
        String cityName = districtService.getNameByCode(address.getCityCode());
        String areaName = districtService.getNameByCode(address.getAreaCode());
        address.setProvinceName(provinceName);
        address.setCityName(cityName);
        address.setAreaName(areaName);
        //uid isDelete
        address.setUid(uid);
        Integer isDefault= count==0?1:0;
        address.setIsDefault(isDefault);
        //补全4项日志
        address.setCreatedUser(username);
        address.setModifiedUser(username);
        address.setCreatedTime(new Date());
        address.setModifiedTime(new Date());

        //插入收货地址
        Integer rows=addressMapper.insert(address);
        if(rows!=1) throw new InsertException("插入用户的收货地址时产生异常");
    }

    @Override
    public List<Address> getByUid(Integer uid) {
        List<Address> list = addressMapper.findByUid(uid);
        return list;
    }
    @Override
    public Address getByAid(Integer aid, Integer uid) {
        // 根据收货地址数据id，查询收货地址详情
        Address address = addressMapper.findByAid(aid);

        if (address == null) {
            throw new AddressNotFoundException("尝试访问的收货地址数据不存在");
        }
        if (!address.getUid().equals(uid)) {
            throw new AccessDeniedException("非法访问");
        }
        address.setProvinceCode(null);
        address.setCityCode(null);
        address.setAreaCode(null);
        address.setCreatedUser(null);
        address.setCreatedTime(null);
        address.setModifiedUser(null);
        address.setModifiedTime(null);
        return address;
    }
}
