package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.ErpDistributionOrderLog;
import com.order.main.mapper.ErpDistributionOrderLogMapper;
import com.order.main.service.IErpDistributionOrderLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * ERP分销订单操作日志服务实现（zhishu 库）
 */
@Service
@RequiredArgsConstructor
@DS("slave")
public class ErpDistributionOrderLogServiceImpl implements IErpDistributionOrderLogService {

    private final ErpDistributionOrderLogMapper baseMapper;

    @Override
    public int write(ErpDistributionOrderLog logEntry) {
        return baseMapper.insert(logEntry);
    }

    @Override
    public List<ErpDistributionOrderLog> listByDistOrderId(Long distOrderId) {
        return baseMapper.selectByDistOrderId(distOrderId);
    }

    @Override
    public int deleteByDistOrderIds(List<Long> distOrderIds) {
        return baseMapper.deleteByDistOrderIds(distOrderIds);
    }

    @Override
    public Date selectLastDispatchTime(Long distOrderId, Date beforeApply) {
        return baseMapper.selectLastDispatchTime(distOrderId, beforeApply);
    }
}
