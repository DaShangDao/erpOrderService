// IExpressDeliveryOrderService.java
package com.order.main.service;

import com.order.main.entity.ExpressDeliveryOrder;

import java.util.List;

public interface IExpressDeliveryOrderService {

    /**
     * 根据业务订单号查询
     */
    ExpressDeliveryOrder getByErpOrderId(String erpOrderId);

    /**
     * 根据快递单号查询
     */
    ExpressDeliveryOrder getByWaybillNo(String waybillNo);

    /**
     * 分页查询列表
     */
    List<ExpressDeliveryOrder> getPageList(ExpressDeliveryOrder query);

    /**
     * 查询总记录数
     */
    int count(ExpressDeliveryOrder query);

    /**
     * 条件查询列表
     */
    List<ExpressDeliveryOrder> getList(ExpressDeliveryOrder query);

    /**
     * 新增
     */
    boolean save(ExpressDeliveryOrder entity);

    /**
     * 更新
     */
    boolean update(ExpressDeliveryOrder entity);

    /**
     * 根据业务订单号删除
     */
    boolean deleteByErpOrderId(String erpOrderId);

    /**
     * 批量删除
     */
    boolean deleteBatch(List<String> erpOrderIds);
}