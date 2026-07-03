// ExpressDeliveryOrderServiceImpl.java
package com.order.main.service.impl;

import com.order.main.entity.ExpressDeliveryOrder;
import com.order.main.mapper.ExpressDeliveryOrderMapper;
import com.order.main.service.IExpressDeliveryOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpressDeliveryOrderServiceImpl implements IExpressDeliveryOrderService {

    private final ExpressDeliveryOrderMapper baseMapper;

    @Override
    public ExpressDeliveryOrder getByErpOrderId(String erpOrderId) {
        return baseMapper.selectByErpOrderId(erpOrderId);
    }

    @Override
    public List<ExpressDeliveryOrder> getByWaybillNo(String waybillNo) {
        return baseMapper.selectByWaybillNo(waybillNo);
    }

    @Override
    public List<ExpressDeliveryOrder> getPageList(ExpressDeliveryOrder query) {
        return baseMapper.selectPageList(query);
    }

    @Override
    public int count(ExpressDeliveryOrder query) {
        return baseMapper.count(query);
    }

    @Override
    public List<ExpressDeliveryOrder> getList(ExpressDeliveryOrder query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ExpressDeliveryOrder entity) {
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ExpressDeliveryOrder entity) {
        return baseMapper.update(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByErpOrderId(String erpOrderId) {
        return baseMapper.deleteByErpOrderId(erpOrderId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<String> erpOrderIds) {
        return baseMapper.deleteBatch(erpOrderIds) > 0;
    }
}