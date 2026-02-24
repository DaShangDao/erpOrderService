package com.order.main.service;

import com.order.main.entity.StockChangeLog;

import java.util.List;

public interface IStockChangeLogService {


    /**
     * 根据关联id查询操作日志
     * @param aboutId  关联id
     * @return
     */
    List<StockChangeLog> selectByAboutId(Long shopGoodsId,String aboutId);

    /**
     * 插入库存变更记录
     * @param stockChangeLog 库存变更记录实体
     * @return 插入成功返回 true，否则返回 false
     */
    boolean insert(StockChangeLog stockChangeLog);
}