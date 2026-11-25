package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * ERP分销订单（分销订单缓冲层）
 *
 * 平台订单 -> ERP分销订单 -> PSI销售订单
 */
@Data
public class ErpDistributionOrder {

    /** 状态：待处理 */
    public static final int STATUS_PENDING = 0;
    /** 状态：分账成功，待下发 PSI */
    public static final int STATUS_SPLIT_OK = 1;
    /** 状态：已下发 PSI */
    public static final int STATUS_PSI_OK = 2;
    /** 状态：下发失败（未分账） */
    public static final int STATUS_FAIL_UNSPLIT = 3;
    /** 状态：下发失败（已分账，PSI 失败） */
    public static final int STATUS_FAIL_SPLIT = 4;
    /** 状态：已退款关闭 */
    public static final int STATUS_REFUNDED = 5;
    /** 状态：已完成 */
    public static final int STATUS_FINISHED = 6;
    /** 状态：已退款待处理（出库已完成，等仓库打单处理） */
    public static final int STATUS_REFUND_PENDING = 7;

    /** 失败环节：分账 */
    public static final int FAIL_STEP_SPLIT = 1;
    /** 失败环节：下发 PSI */
    public static final int FAIL_STEP_PSI = 2;

    private Long id;
    private String orderNo;
    private Long sourceOrderId;
    private String sourceOrderSn;
    private Long queueId;

    private Long ownerId;
    private Long sellerId;
    private String buyerPhone;
    private String sellerPhone;

    private Long shopErpId;
    private String shopErpName;
    private Integer shopType;
    private Long productId;
    private String barcode;
    private String productName;
    private String appearance;

    private Integer itemSeq;
    private Integer quantity;
    private Long unitPrice;
    private Long freight;
    private Long bookPrice;
    private Long platformFee;
    private Long warehouseFee;
    private Long totalAmount;

    private Long splitConfigId;
    private String splitConfigName;
    private String splitRule;

    private Long psiOrderId;
    private String psiOrderNo;
    private Integer failStep;
    private Integer retryCount;

    private Integer status;
    private String msg;
    private String itemList;

    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
    private String delFlag;
    private String tenantId;
    private Long createDept;
}
