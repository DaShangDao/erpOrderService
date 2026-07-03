package com.order.main.entity;

import lombok.Data;

/**
 * 订单队列实体对象 t_erp_goods_order_queue
 *
 * @author yxy
 * @date 2026-05-26
 */
@Data
public class ErpGoodsOrderQueue {

    /**
     * 主键ID（自增）
     */
    private Long id;

    /**
     * 订单id
     */
    private Long erpGoodsOrderId;

    /**
     * 状态：0 未发布，1 成功，2 失败
     */
    private String status;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 使用时间
     */
    private Long useTime;

    /**
     * 日志
     */
    private String msg;

    /**
     * 订单类型 0 订单创建  1 订单完成  2 订单退款
     */
    private String orderType;

    /**
     * 分页参数
     */
    private int pageNum;
    private int pageSize;
}