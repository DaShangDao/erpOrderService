package com.order.main.mapper;

import com.order.main.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 店铺主表Mapper接口
 *
 * @author yxy
 * @date 2025-03-10
 */
@Mapper
public interface UserRoleMapper {

    /**
     * 查询用户是否存在 “入驻书店” 角色
     * @param userId
     * @return
     */
    int selecUserRole(Long userId);

}
