package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ErpGoodsOrderQueue;
import com.order.main.mapper.ErpGoodsOrderQueueMapper;
import com.order.main.service.IErpGoodsOrderQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ErpGoodsOrderQueueServiceImpl implements IErpGoodsOrderQueueService {

    private final ErpGoodsOrderQueueMapper baseMapper;

    @Override
    @DS("master")
    public ErpGoodsOrderQueue getById(Long id) {
        return baseMapper.selectById(id);
    }

    /**
     * 获取未下发的订单数据
     * @return
     */
    @Override
    @DS("master")
    public ErpGoodsOrder selectByStatus(){
        return baseMapper.selectByStatus();
    }

    @Override
    @DS("master")
    public List<ErpGoodsOrderQueue> getPageList(ErpGoodsOrderQueue queue) {
        queue.setPageNum((queue.getPageNum() - 1) * queue.getPageSize());
        return baseMapper.selectPageList(queue);
    }

    @Override
    @DS("master")
    public int count(ErpGoodsOrderQueue queue) {
        return baseMapper.count(queue);
    }

    @Override
    @DS("master")
    public List<ErpGoodsOrderQueue> getList(ErpGoodsOrderQueue query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DS("master")
    public boolean save(ErpGoodsOrderQueue queue) {
        // 自动添加创建时间（当前时间戳，单位：秒）
        if (queue.getCreateTime() == null) {
            queue.setCreateTime(System.currentTimeMillis() / 1000);
        }
        return baseMapper.insert(queue) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DS("master")
    public boolean update(ErpGoodsOrderQueue queue) {
        // 自动添加使用时间（当前时间戳，单位：秒）
        queue.setUseTime(System.currentTimeMillis() / 1000);
        return baseMapper.update(queue) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DS("master")
    public boolean deleteById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DS("master")
    public boolean deleteBatch(List<Long> ids) {
        return baseMapper.deleteBatch(ids) > 0;
    }
}