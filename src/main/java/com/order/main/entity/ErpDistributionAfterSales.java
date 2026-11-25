package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * ERP分销订单售后单
 */
@Data
public class ErpDistributionAfterSales {

    /** 待仓库确认 */
    public static final int STATUS_WAIT_WAREHOUSE = 0;
    /** 仓库已确认，待分销确认 */
    public static final int STATUS_WAIT_DISTRIBUTOR = 1;
    /** 执行中 */
    public static final int STATUS_EXECUTING = 2;
    /** 已完成 */
    public static final int STATUS_DONE = 3;
    /** 已关闭 */
    public static final int STATUS_CLOSED = 4;

    private Long id;
    private Long distOrderId;
    private String distOrderNo;
    private Long sourceOrderId;
    private String sourceOrderSn;
    private Long ownerId;
    private Long sellerId;
    private Long productId;
    private String productName;
    private String reason;
    private Long refundAmount;
    private Long maxAmount;
    private Integer status;
    private Date applyTime;
    private Date warehouseTime;
    private Date distributorTime;
    private Date finishTime;
    private String warehouseRemark;
    private String distributorRemark;
    private Integer rejectCount;
    private Integer autoFlag;

    /** 来源 1-平台退款 2-ERP主动发起 3-仓库发起（缺货） */
    private Integer sourceType;

    /** 非数据库字段：仓库发起售后时的出库单ID（用于同步移除出库明细） */
    private Long outboundOrderId;
    private Date createTime;
    private Date updateTime;
    private String delFlag;
}
