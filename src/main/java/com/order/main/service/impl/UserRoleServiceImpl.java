package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.Shop;
import com.order.main.mapper.ShopMapper;
import com.order.main.mapper.UserRoleMapper;
import com.order.main.service.IShopService;
import com.order.main.service.IUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 店铺主表Service业务层处理
 *
 * @author yxy
 * @date 2025-03-10
 */
@RequiredArgsConstructor
@Service
public class UserRoleServiceImpl implements IUserRoleService {

    private final UserRoleMapper userRoleMapper;


    @Override
    @DS("slave")
    public int selecUserRole(Long userId){
        return  userRoleMapper.selecUserRole(userId);
    }

}
