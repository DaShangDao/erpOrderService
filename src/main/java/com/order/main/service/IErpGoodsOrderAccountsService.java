// IErpGoodsOrderAccountsService.java
package com.order.main.service;

import com.order.main.entity.ErpGoodsOrderAccounts;

import java.util.List;

public interface IErpGoodsOrderAccountsService {

    /**
     * 根据ID查询
     */
    ErpGoodsOrderAccounts getById(Long id);

    /**
     * 根据订单ID查询
     */
    List<ErpGoodsOrderAccounts> getByOrderId(Long erpOrderId);

    /**
     * 分页查询列表
     */
    List<ErpGoodsOrderAccounts> getPageList(ErpGoodsOrderAccounts query);

    /**
     * 查询总记录数
     */
    int count(ErpGoodsOrderAccounts query);

    /**
     * 条件查询列表
     */
    List<ErpGoodsOrderAccounts> getList(ErpGoodsOrderAccounts query);

    /**
     * 新增
     */
    boolean save(ErpGoodsOrderAccounts entity);

    /**
     * 更新
     */
    boolean update(ErpGoodsOrderAccounts entity);

    /**
     * 根据ID删除
     */
    boolean deleteById(Long id);

    /**
     * 批量删除
     */
    boolean deleteBatch(List<Long> ids);
}