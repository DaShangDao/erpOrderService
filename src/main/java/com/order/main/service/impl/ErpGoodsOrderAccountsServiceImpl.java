// ErpGoodsOrderAccountsServiceImpl.java
package com.order.main.service.impl;

import com.order.main.entity.ErpGoodsOrderAccounts;
import com.order.main.mapper.ErpGoodsOrderAccountsMapper;
import com.order.main.service.IErpGoodsOrderAccountsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ErpGoodsOrderAccountsServiceImpl implements IErpGoodsOrderAccountsService {

    private final ErpGoodsOrderAccountsMapper baseMapper;

    @Override
    public ErpGoodsOrderAccounts getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<ErpGoodsOrderAccounts> getByOrderId(Long erpOrderId) {
        return baseMapper.selectByOrderId(erpOrderId);
    }

    @Override
    public List<ErpGoodsOrderAccounts> getPageList(ErpGoodsOrderAccounts query) {
        return baseMapper.selectPageList(query);
    }

    @Override
    public int count(ErpGoodsOrderAccounts query) {
        return baseMapper.count(query);
    }

    @Override
    public List<ErpGoodsOrderAccounts> getList(ErpGoodsOrderAccounts query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ErpGoodsOrderAccounts entity) {
        return baseMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ErpGoodsOrderAccounts entity) {
        return baseMapper.update(entity) > 0;
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