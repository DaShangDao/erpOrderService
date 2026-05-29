package com.order.main.service;

import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ErpGoodsOrderQueue;

import java.util.List;

public interface IErpGoodsOrderQueueService {

    /**
     * 根据ID查询队列记录
     */
    ErpGoodsOrderQueue getById(Long id);

    /**
     * 获取未下发的订单数据
     * @return
     */
    ErpGoodsOrder selectByStatus();

    /**
     * 分页查询队列列表
     */
    List<ErpGoodsOrderQueue> getPageList(ErpGoodsOrderQueue queue);

    /**
     * 查询总记录数（用于分页）
     */
    int count(ErpGoodsOrderQueue queue);

    /**
     * 查询所有队列列表
     */
    List<ErpGoodsOrderQueue> getList(ErpGoodsOrderQueue query);

    /**
     * 新增队列记录
     */
    boolean save(ErpGoodsOrderQueue queue);

    /**
     * 更新队列记录
     */
    boolean update(ErpGoodsOrderQueue queue);

    /**
     * 根据ID删除队列记录
     */
    boolean deleteById(Long id);

    /**
     * 批量删除队列记录
     */
    boolean deleteBatch(List<Long> ids);
}