package com.order.main.service;

import com.order.main.entity.ErpDistributionOrderLog;

import java.util.Date;
import java.util.List;

/**
 * ERP分销订单操作日志服务（zhishu 库）
 */
public interface IErpDistributionOrderLogService {

    int write(ErpDistributionOrderLog logEntry);

    List<ErpDistributionOrderLog> listByDistOrderId(Long distOrderId);

    int deleteByDistOrderIds(List<Long> distOrderIds);

    /** 最近一次成功下发（自动/手动）时间 */
    Date selectLastDispatchTime(Long distOrderId, Date beforeApply);
}
