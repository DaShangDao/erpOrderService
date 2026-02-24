// ErpGoodsOrderAccountsMapper.java
package com.order.main.mapper;

import com.order.main.entity.ErpGoodsOrderAccounts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ErpGoodsOrderAccountsMapper {

    /**
     * 根据ID查询
     */
    ErpGoodsOrderAccounts selectById(Long id);

    /**
     * 根据订单ID查询
     */
    List<ErpGoodsOrderAccounts> selectByOrderId(Long erpOrderId);

    /**
     * 分页查询列表
     */
    List<ErpGoodsOrderAccounts> selectPageList(ErpGoodsOrderAccounts query);

    /**
     * 查询总记录数
     */
    int count(ErpGoodsOrderAccounts query);

    /**
     * 条件查询列表
     */
    List<ErpGoodsOrderAccounts> selectList(ErpGoodsOrderAccounts query);

    /**
     * 新增
     */
    int insert(ErpGoodsOrderAccounts entity);

    /**
     * 更新
     */
    int update(ErpGoodsOrderAccounts entity);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 批量删除
     */
    int deleteBatch(@Param("ids") List<Long> ids);
}