package com.order.main.service;

import com.order.main.entity.ErpDistributionOrder;

import java.util.List;

/**
 * ERP分销订单服务（zhishu 库）
 */
public interface IErpDistributionOrderService {

    /**
     * 新增 ERP 分销订单
     */
    int create(ErpDistributionOrder order);

    ErpDistributionOrder getById(Long id);

    ErpDistributionOrder getByOrderNo(String orderNo);

    ErpDistributionOrder getBySourceAndItem(Long sourceOrderId, Long productId, Integer itemSeq);

    List<ErpDistributionOrder> listBySourceOrderId(Long sourceOrderId);

    /**
     * 分账成功
     */
    int markSplitOk(Long id);

    /**
     * 下发 PSI 成功
     */
    int markPsiOk(Long id, Long psiOrderId, String psiOrderNo);

    /**
     * 下发失败
     *
     * @param id       ERP 分销订单ID
     * @param status   STATUS_FAIL_UNSPLIT / STATUS_FAIL_SPLIT
     * @param failStep FAIL_STEP_SPLIT / FAIL_STEP_PSI
     * @param msg      失败原因
     */
    int markFail(Long id, int status, int failStep, String msg);

    /**
     * 按来源平台订单置已退款关闭
     */
    int markRefundedBySourceOrder(Long sourceOrderId);

    /**
     * 按来源平台订单置已完成（仅已下发的行）
     */
    int markFinishedBySourceOrder(Long sourceOrderId);

    int incrementRetry(Long id);

    /** 按ID更新状态 */
    int updateStatusById(Long id, Integer status, String msg);

    /**
     * 手动下发：更新商品与金额信息（换商品重新计算后落库）
     */
    int updateDispatchInfo(ErpDistributionOrder order);

    /** 清理：查询可删除的订单ID（create_time < cutoff 且 status 在列表内） */
    java.util.List<Long> listExpiredIds(java.util.Date cutoff, java.util.List<Integer> statusList, int limit);

    /** 清理：按ID物理删除订单 */
    int deleteByIds(java.util.List<Long> ids);

    /** 清理：统计超期未完成订单数（status 0/1） */
    long countStuckBefore(java.util.Date cutoff);
}
