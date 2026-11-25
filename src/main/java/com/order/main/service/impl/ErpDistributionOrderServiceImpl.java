package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.ErpDistributionOrder;
import com.order.main.mapper.ErpDistributionOrderMapper;
import com.order.main.service.IErpDistributionOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ERP分销订单服务实现（zhishu 库）
 */
@Service
@RequiredArgsConstructor
@DS("slave")
public class ErpDistributionOrderServiceImpl implements IErpDistributionOrderService {

    private final ErpDistributionOrderMapper baseMapper;

    @Override
    public int create(ErpDistributionOrder order) {
        return baseMapper.insert(order);
    }

    @Override
    public ErpDistributionOrder getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public ErpDistributionOrder getByOrderNo(String orderNo) {
        return baseMapper.selectByOrderNo(orderNo);
    }

    @Override
    public ErpDistributionOrder getBySourceAndItem(Long sourceOrderId, Long productId, Integer itemSeq) {
        return baseMapper.selectBySourceAndItem(sourceOrderId, productId, itemSeq);
    }

    @Override
    public List<ErpDistributionOrder> listBySourceOrderId(Long sourceOrderId) {
        return baseMapper.selectBySourceOrderId(sourceOrderId);
    }

    @Override
    public int markSplitOk(Long id) {
        return baseMapper.updateStatus(id, ErpDistributionOrder.STATUS_SPLIT_OK, null, null, null, null, null);
    }

    @Override
    public int markPsiOk(Long id, Long psiOrderId, String psiOrderNo) {
        return baseMapper.updateStatus(id, ErpDistributionOrder.STATUS_PSI_OK, null, null,
                psiOrderId, psiOrderNo, null);
    }

    @Override
    public int markFail(Long id, int status, int failStep, String msg) {
        return baseMapper.updateStatus(id, status, msg, failStep, null, null, null);
    }

    @Override
    public int markRefundedBySourceOrder(Long sourceOrderId) {
        return baseMapper.updateStatusBySourceOrder(sourceOrderId,
                java.util.Arrays.asList(
                        ErpDistributionOrder.STATUS_PENDING,
                        ErpDistributionOrder.STATUS_SPLIT_OK,
                        ErpDistributionOrder.STATUS_PSI_OK,
                        ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                        ErpDistributionOrder.STATUS_FAIL_SPLIT),
                ErpDistributionOrder.STATUS_REFUNDED,
                "平台订单退款，已关闭");
    }

    @Override
    public int markFinishedBySourceOrder(Long sourceOrderId) {
        return baseMapper.updateStatusBySourceOrder(sourceOrderId,
                java.util.Collections.singletonList(ErpDistributionOrder.STATUS_PSI_OK),
                ErpDistributionOrder.STATUS_FINISHED,
                "订单已完成");
    }

    @Override
    public int updateStatusById(Long id, Integer status, String msg) {
        return baseMapper.updateStatus(id, status, msg, null, null, null, null);
    }

    @Override
    public int incrementRetry(Long id) {
        return baseMapper.incrementRetry(id);
    }

    @Override
    public java.util.List<Long> listExpiredIds(java.util.Date cutoff, java.util.List<Integer> statusList, int limit) {
        return baseMapper.selectExpiredIds(cutoff, statusList, limit);
    }

    @Override
    public int deleteByIds(java.util.List<Long> ids) {
        return baseMapper.deleteByIds(ids);
    }

    @Override
    public long countStuckBefore(java.util.Date cutoff) {
        return baseMapper.countStuckBefore(cutoff);
    }

    @Override
    public int updateDispatchInfo(ErpDistributionOrder order) {
        return baseMapper.updateDispatchInfo(order);
    }
}
