package com.order.main.service;

import com.order.main.entity.SynchronizationShopLog;
import java.util.List;

public interface ISynchronizationShopLogService {

    /**
     * 根据ID查询同步日志
     */
    SynchronizationShopLog getById(Long id);

    /**
     * 根据商品ID查询同步日志
     */
    List<SynchronizationShopLog> getListByGoodsId(Long goodsId);

    /**
     * 根据店铺ID查询同步日志
     */
    List<SynchronizationShopLog> getListByShopId(Long shopId);

    /**
     * 根据ERP订单ID查询同步日志
     */
    List<SynchronizationShopLog> getListByErpOrderId(Long erpOrderId);

    /**
     * 分页查询同步日志列表
     */
    List<SynchronizationShopLog> getPageList(SynchronizationShopLog log);

    /**
     * 查询总记录数（用于分页）
     */
    int count(SynchronizationShopLog log);

    /**
     * 查询所有同步日志列表
     */
    List<SynchronizationShopLog> getList(SynchronizationShopLog query);

    /**
     * 新增同步日志
     */
    boolean save(SynchronizationShopLog log);

    /**
     * 批量新增同步日志
     */
    boolean saveBatch(List<SynchronizationShopLog> list);

    /**
     * 更新同步日志
     */
    boolean update(SynchronizationShopLog log);

    /**
     * 根据ID删除同步日志
     */
    boolean deleteById(Long id);

    /**
     * 批量删除同步日志
     */
    boolean deleteBatch(List<Long> ids);
}