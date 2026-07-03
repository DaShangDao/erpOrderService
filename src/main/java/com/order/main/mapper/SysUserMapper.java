package com.order.main.mapper;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.SysUser;
import org.apache.ibatis.annotations.*;

/**
 * 用户表 数据层
 *
 * @author Lion Li
 */
@DS("slave")
@Mapper
public interface SysUserMapper  {

    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    SysUser selectUserOne(Long userId);

    /**
     * 修改用户金额
     * @param sysUser
     * @return
     */
    int updateMoney(SysUser sysUser);
}
