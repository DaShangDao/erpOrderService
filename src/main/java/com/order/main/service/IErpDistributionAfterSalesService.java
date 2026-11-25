package com.order.main.service;

import com.order.main.entity.ErpDistributionAfterSales;

import java.util.List;
import java.util.Map;

/**
 * ERP分销订单售后
 */
public interface IErpDistributionAfterSalesService {

    /** 分账明细：只返回分销商自己的资金流水（入账/出账/冻结） */
    Map listSplitLogs(Long distOrderId);

    /** 仓库方发起售后（缺货）：全额退款，免分销商确认；rollbackStock=true 回滚库存，false 库存清零报损 */
    Map warehouseApply(String orderNo, Long sellerId, String reason, boolean rollbackStock, Long outboundOrderId);

    /** 分销商申请售后 */
    Map apply(Long distOrderId, Long ownerId, String reason);

    /** 仓库方确认并填写退款金额 */
    Map warehouseConfirm(Long id, Long sellerId, Long refundAmount, String remark);

    /** 分销商确认（执行回滚） */
    Map distributorConfirm(Long id, Long ownerId, String remark);

    /** 分销商不同意，退回仓库方重填 */
    Map distributorReject(Long id, Long ownerId, String remark);

    /** 分销商取消售后（仓库已出库/已发货场景，仓库确认前可撤销） */
    Map cancel(Long id, Long ownerId, String remark);

    /** 场景一：平台退款自动回滚（全额书价） */
    Map autoRefund(Long distOrderId, boolean rollbackStock);

    /** 已退款待处理：仓库处理后回滚资金（可选库存） */
    Map handlePendingRefund(Long distOrderId, Long sellerId, boolean rollbackStock);

    /** 平台退款到达时记录一条售后（source_type=1，执行中），锁定「售后申请时间」 */
    void recordPlatformRefund(Long distOrderId);

    /** 7 天超时自动确认 */
    int handleTimeout();

    List<ErpDistributionAfterSales> listByDistOrderId(Long distOrderId);

    /** 仓库方待处理售后列表（status=0） */
    List<ErpDistributionAfterSales> listPendingBySeller(Long sellerId);

    ErpDistributionAfterSales getById(Long id);

    /** 该分销单已售后完成过的仓库（seller_id，去重）：这些仓库不允许再被重新选中下发 */
    List<Long> listRefundedSellerIds(Long distOrderId);
}
