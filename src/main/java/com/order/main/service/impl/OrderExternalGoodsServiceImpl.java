package com.order.main.service.impl;

import com.order.main.dto.OrderExternalGoodsDto;
import com.order.main.entity.OrderExternalGoods;
import com.order.main.mapper.OrderExternalGoodsMapper;
import com.order.main.service.IOrderExternalGoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 外部订单关联的订单与erp商品Service业务层处理
 *
 * @author yxy
 * @date 2025-12-09
 */
@RequiredArgsConstructor
@Service
public class OrderExternalGoodsServiceImpl implements IOrderExternalGoodsService {

    private final OrderExternalGoodsMapper baseMapper;

    /**
     * 根据ID查询
     */
    @Override
    public OrderExternalGoods selectById(Long id) {
        return baseMapper.selectById(id);
    }

    /**
     * 根据订单id查询
     * @param orderId
     * @return
     */
    @Override
    public OrderExternalGoods selectByOrderId(Long orderId){
        return baseMapper.selectByOrderId(orderId);
    }

    /**
     * 根据仓库用户id查询订单信息
     * @param orderExternalGoodsDto
     * @return
     */
    @Override
    public List<OrderExternalGoodsDto> selelctListBydeptUseId(OrderExternalGoodsDto orderExternalGoodsDto){
        orderExternalGoodsDto.setPageNum((orderExternalGoodsDto.getPageNum()-1)*orderExternalGoodsDto.getPageSize());
        return baseMapper.selelctListBydeptUseId(orderExternalGoodsDto);
    }

    /**
     * 根据仓库用户id查询订单信息总数
     * @param orderExternalGoodsDto
     * @return
     */
    @Override
    public int selelctTotalBydeptUseId(OrderExternalGoodsDto orderExternalGoodsDto){
        return baseMapper.selelctTotalBydeptUseId(orderExternalGoodsDto);
    }

    /**
     * 根据订单id和是否分销查询
     * @param orderId
     * @param isDistribution
     * @return
     */
    public OrderExternalGoods selectByOrderIdAndIsDistribution(Long orderId, String isDistribution){
        return baseMapper.selectByOrderIdAndIsDistribution(orderId, isDistribution);
    }

    /**
     * 查询列表
     */
    @Override
    public List<OrderExternalGoods> selectList(OrderExternalGoods orderExternalGoods) {
        return baseMapper.selectList(orderExternalGoods);
    }

    /**
     * 新增
     */
    @Override
    public int insert(OrderExternalGoods orderExternalGoods) {
        // 设置创建时间（时间戳）
        if (orderExternalGoods.getCreatedAt() == null) {
            orderExternalGoods.setCreatedAt(System.currentTimeMillis());
        }
        return baseMapper.insert(orderExternalGoods);
    }

    /**
     * 修改
     */
    @Override
    public int update(OrderExternalGoods orderExternalGoods) {
        return baseMapper.update(orderExternalGoods);
    }

    /**
     * 根据ID删除
     */
    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    /**
     * 根据订单id删除
     */
    @Override
    public int deletByOrderId(Long orderId){
        return baseMapper.deletByOrderId(orderId);
    }

    /**
     * 批量删除
     */
    @Override
    public int deleteByIds(Long[] ids) {
        return baseMapper.deleteByIds(ids);
    }
}