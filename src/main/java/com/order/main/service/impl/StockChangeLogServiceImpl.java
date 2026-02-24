package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.StockChangeLog;
import com.order.main.mapper.StockChangeLogMapper;
import com.order.main.service.IStockChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockChangeLogServiceImpl implements IStockChangeLogService {

    private final StockChangeLogMapper baseMapper;

    /**
     * 根据关联id查询操作日志
     * @param aboutId  关联id
     * @return
     */
    @Override
    @DS("slave")
    public List<StockChangeLog> selectByAboutId(Long shopGoodsId,String aboutId){
        return baseMapper.selectByAboutId(shopGoodsId,aboutId);
    }

    @Override
    @DS("slave")
    public boolean insert(StockChangeLog stockChangeLog) {
        // 设置默认值（如果实体中没有设置）
        if (stockChangeLog.getCreateTime() == null) {
            stockChangeLog.setCreateTime(new Date());
        }
        if (stockChangeLog.getUpdateTime() == null) {
            stockChangeLog.setUpdateTime(new Date());
        }
        if (stockChangeLog.getDelFlag() == null) {
            stockChangeLog.setDelFlag("0"); // 默认未删除
        }
        // 执行插入操作
        int result = baseMapper.insert(stockChangeLog);
        return result > 0;
    }
}