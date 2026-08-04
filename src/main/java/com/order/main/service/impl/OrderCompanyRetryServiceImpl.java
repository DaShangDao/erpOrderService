package com.order.main.service.impl;

import com.order.main.entity.OrderCompanyRetry;
import com.order.main.mapper.OrderCompanyRetryMapper;
import com.order.main.service.IOrderCompanyRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCompanyRetryServiceImpl implements IOrderCompanyRetryService {

    private final OrderCompanyRetryMapper baseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(OrderCompanyRetry entity) {
        return baseMapper.insert(entity) > 0;
    }

    @Override
    public List<OrderCompanyRetry> getPendingList() {
        return baseMapper.selectPendingList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRetryResult(OrderCompanyRetry entity) {
        return baseMapper.updateRetryResult(entity) > 0;
    }
}
