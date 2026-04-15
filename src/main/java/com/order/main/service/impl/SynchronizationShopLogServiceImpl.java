package com.order.main.service.impl;

import com.order.main.entity.SynchronizationShopLog;
import com.order.main.mapper.SynchronizationShopLogMapper;
import com.order.main.service.ISynchronizationShopLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SynchronizationShopLogServiceImpl implements ISynchronizationShopLogService {

    private final SynchronizationShopLogMapper baseMapper;

    @Override
    public SynchronizationShopLog getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<SynchronizationShopLog> getListByGoodsId(Long goodsId) {
        return baseMapper.getListByGoodsId(goodsId);
    }

    @Override
    public List<SynchronizationShopLog> getListByShopId(Long shopId) {
        return baseMapper.getListByShopId(shopId);
    }

    @Override
    public List<SynchronizationShopLog> getListByErpOrderId(Long erpOrderId) {
        return baseMapper.getListByErpOrderId(erpOrderId);
    }

    @Override
    public List<SynchronizationShopLog> getPageList(SynchronizationShopLog log) {
        log.setPageNum((log.getPageNum() - 1) * log.getPageSize());
        return baseMapper.selectPageList(log);
    }

    @Override
    public int count(SynchronizationShopLog log) {
        return baseMapper.count(log);
    }

    @Override
    public List<SynchronizationShopLog> getList(SynchronizationShopLog query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SynchronizationShopLog log) {
        return baseMapper.insert(log) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<SynchronizationShopLog> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        return baseMapper.insertBatch(list) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SynchronizationShopLog log) {
        return baseMapper.update(log) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Long> ids) {
        return baseMapper.deleteBatch(ids) > 0;
    }
}