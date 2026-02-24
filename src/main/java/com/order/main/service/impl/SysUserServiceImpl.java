package com.order.main.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.SysUser;
import com.order.main.mapper.SysUserMapper;
import com.order.main.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
/**
 * 用户 业务层处理
 *
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    @Override
    @DS("slave")
    public SysUser selectUserOne(Long userId){
        return sysUserMapper.selectUserOne(userId);
    }

    /**
     * 修改用户金额
     * @param sysUser
     * @return
     */
    @Override
    @DS("slave")
    public int updateMoney(SysUser sysUser){
        return sysUserMapper.updateMoney(sysUser);
    }

}
