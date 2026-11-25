package com.order.main.service;


import com.order.main.entity.SysUser;

/**
 * 用户 业务层
 *
 */
public interface ISysUserService {


    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    SysUser selectUserOne(Long userId);

    /**
     * 查询卖家展示名：店铺名 > 昵称 > 账号
     */
    String selectShopName(Long userId);

    /**
     * 修改用户金额
     * @param sysUser
     * @return
     */
    int updateMoney(SysUser sysUser);
}
