package com.order.main.mapper;

import com.order.main.dto.OrderExternalGoodsDto;
import com.order.main.entity.OrderExternalGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 外部订单关联的订单与erp商品Mapper接口
 *
 * @author yxy
 * @date 2025-12-09
 */
@Mapper
public interface OrderExternalGoodsMapper {

    /**
     * 根据ID查询
     */
    OrderExternalGoods selectById(Long id);

    /**
     * 根据订单id查询
     * @param orderId
     * @return
     */
    OrderExternalGoods selectByOrderId(Long orderId);

    /**
     * 根据仓库用户id查询订单信息
     * @param orderExternalGoodsDto
     * @return
     */
    List<OrderExternalGoodsDto> selelctListBydeptUseId(OrderExternalGoodsDto orderExternalGoodsDto);

    /**
     * 根据仓库用户id查询订单信息总数
     * @param orderExternalGoodsDto
     * @return
     */
    int selelctTotalBydeptUseId(OrderExternalGoodsDto orderExternalGoodsDto);

    /**
     * 根据订单id和是否分销查询
     * @param orderId
     * @param isDistribution
     * @return
     */
    OrderExternalGoods selectByOrderIdAndIsDistribution(@Param("orderId") Long orderId,@Param("isDistribution") String isDistribution);

    /**
     * 查询列表
     */
    List<OrderExternalGoods> selectList(OrderExternalGoods orderExternalGoods);

    /**
     * 新增
     */
    int insert(OrderExternalGoods orderExternalGoods);

    /**
     * 修改
     */
    int update(OrderExternalGoods orderExternalGoods);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 根据订单id删除
     */
    int deletByOrderId(Long orderId);

    /**
     * 批量删除
     */
    int deleteByIds(Long[] ids);
}