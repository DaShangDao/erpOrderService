package com.order.main.service;

import com.order.main.entity.Shop;

import java.util.List;


/**
 * 店铺主表Service接口
 *
 * @author yxy
 * @date 2025-03-10
 */
public interface IUserRoleService {

    /**
     * 查询用户是否存在 “入驻书店” 角色
     * @param userId
     * @return
     */
    int selecUserRole(Long userId);
}
