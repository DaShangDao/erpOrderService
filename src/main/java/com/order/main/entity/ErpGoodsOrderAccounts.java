// ErpGoodsOrderAccounts.java
package com.order.main.entity;

import lombok.Data;

/**
 * 商品订单账户转账记录实体类
 * 对应表：erp_goods_order_accounts
 */
@Data
public class ErpGoodsOrderAccounts {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long erpOrderId;

    /**
     * 转出账户ID
     */
    private Long getId;

    /**
     * 转入账户ID
     */
    private Long setId;

    /**
     * 转出任务状态
     */
    private Integer getStatus;

    /**
     * 转入任务状态
     */
    private Integer setStatus;
}