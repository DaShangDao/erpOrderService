package com.order.main.entity;

import lombok.Data;

/**
 * 外部订单关联的订单与erp商品对象 order_external_goods
 *
 * @author yxy
 * @date 2025-12-09
 */
@Data
public class OrderExternalGoods  {

    /**
     * 
     */
    private Long id;

    /**
     * 订单类型  1 内部订单   2 外部订单
     */
    private Long type;

    /**
     * 订单id
     */
    private Long orderId;

    /**
     * erp自营商品id
     */
    private Long goodsId;

    /**
     * 店家支付金额（不包含手续费）
     */
    private Long payPrice;

    /**
     * 手续费
     */
    private Long serviceCharge;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间时间戳
     */
    private Long createdAt;

    /**
     * 是否完成分销 0否 1是
     */
    private String isDistribution;

    /**
     * 仓库的创建人
     */
    private Long deptUseId;

    /**
     * 是否出库  0 未出库  1 出库
     */
    private Long whetherOutbound;

}
