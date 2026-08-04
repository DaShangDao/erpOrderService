package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * 拼多多发货网络超时重试记录
 * 对应表 order_company_retry
 */
@Data
public class OrderCompanyRetry {

    /**
     * 主键，自增
     */
    private Long id;

    /**
     * 店铺ID(拼多多店铺)
     */
    private Long shopId;

    /**
     * ERP订单ID
     */
    private Long erpOrderId;

    /**
     * 拼多多订单号
     */
    private String orderSn;

    /**
     * 快递单号
     */
    private String trackingNumber;

    /**
     * 快递公司名称
     */
    private String companyName;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 状态: 0待重试 1成功 2失败(其他错误)
     */
    private Integer status;

    /**
     * 最近一次调用返回结果
     */
    private String lastResult;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
