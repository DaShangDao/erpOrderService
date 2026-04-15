package com.order.main.entity;

import lombok.Data;

/**
 * 同步店铺商品库存日志对象 synchronization_shop_log
 *
 * @author yxy
 * @date 2026-04-14
 */
@Data
public class SynchronizationShopLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 商品id
     */
    private Long goodsId;

    /**
     * 商品的创建人
     */
    private Long goodsCreateBy;

    /**
     * erp订单id
     */
    private Long erpOrderId;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺的创建人
     */
    private Long shopCreateBy;

    /**
     * 更新后库存
     */
    private Integer inventory;

    /**
     * 更新前库存
     */
    private Integer inventoryOld;

    /**
     * 三方平台id
     */
    private Long platformId;

    /**
     * 状态码 200 成功 500 失败
     */
    private String code;

    /**
     * 日志信息
     */
    private String msg;

    /**
     * 创建时间
     */
    private Long createAt;

    /**
     * 店铺数量
     */
    private String shopNum;

    /**
     * 操作类型
     */
    private String updateType;

    /**
     * 店铺类型
     */
    private String shopType;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 分页参数
     */
    private int pageNum;
    private int pageSize;
}