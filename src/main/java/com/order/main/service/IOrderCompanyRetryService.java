package com.order.main.service;

import com.order.main.entity.OrderCompanyRetry;

import java.util.List;

public interface IOrderCompanyRetryService {

    /**
     * 新增重试记录
     */
    boolean save(OrderCompanyRetry entity);

    /**
     * 查询待重试记录（status=0）
     */
    List<OrderCompanyRetry> getPendingList();

    /**
     * 更新重试结果
     */
    boolean updateRetryResult(OrderCompanyRetry entity);
}
