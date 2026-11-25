package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * ERP分销订单操作日志（zhishu 库，只追加）
 *
 * 一次操作一条记录，记录订单生命周期、下发对象、资金状态、售后状态。
 */
@Data
public class ErpDistributionOrderLog {

    /** 分类：订单 */
    public static final int CATEGORY_ORDER = 1;
    /** 分类：下发 */
    public static final int CATEGORY_DISPATCH = 2;
    /** 分类：资金 */
    public static final int CATEGORY_FUND = 3;
    /** 分类：售后 */
    public static final String ACTION_AS_WAREHOUSE_CONFIRM = "AS_WAREHOUSE_CONFIRM";
    public static final String ACTION_AS_DISTRIBUTOR_CONFIRM = "AS_DISTRIBUTOR_CONFIRM";
    public static final String ACTION_AS_DISTRIBUTOR_REJECT = "AS_DISTRIBUTOR_REJECT";
    public static final String ACTION_AS_TIMEOUT_CONFIRM = "AS_TIMEOUT_CONFIRM";
    public static final String ACTION_AS_AUTO_REFUND = "AS_AUTO_REFUND";
    public static final String ACTION_AS_DONE = "AS_DONE";

    public static final int CATEGORY_AFTER_SALES = 4;

    public static final String ACTION_ORDER_CREATE = "ORDER_CREATE";
    public static final String ACTION_ORDER_FINISH = "ORDER_FINISH";
    public static final String ACTION_ORDER_CLOSE = "ORDER_CLOSE";

    public static final String ACTION_DISPATCH_BEGIN = "DISPATCH_BEGIN";
    public static final String ACTION_DISPATCH_AUTO = "DISPATCH_AUTO";
    public static final String ACTION_DISPATCH_MANUAL = "DISPATCH_MANUAL";
    public static final String ACTION_DISPATCH_FAIL = "DISPATCH_FAIL";
    public static final String ACTION_DISPATCH_RETRY = "DISPATCH_RETRY";

    public static final String ACTION_SETTLE_BEGIN = "SETTLE_BEGIN";
    public static final String ACTION_SETTLE = "SETTLE";
    public static final String ACTION_SETTLE_FAIL = "SETTLE_FAIL";
    public static final String ACTION_REFUND_BEGIN = "REFUND_BEGIN";
    public static final String ACTION_REFUND = "REFUND";
    public static final String ACTION_REFUND_FAIL = "REFUND_FAIL";

    public static final String ACTION_AS_APPLY = "AS_APPLY";
    public static final String ACTION_AS_WAREHOUSE_APPLY = "AS_WAREHOUSE_APPLY";
    public static final String ACTION_AS_AGREE = "AS_AGREE";
    public static final String ACTION_AS_REJECT = "AS_REJECT";
    public static final String ACTION_AS_RECEIVED = "AS_RECEIVED";
    public static final String ACTION_AS_REFUND_DONE = "AS_REFUND_DONE";
    public static final String ACTION_AS_CLOSE = "AS_CLOSE";

    /** 资金状态：未结算 */
    public static final int SETTLE_NONE = 0;
    /** 资金状态：冻结中 */
    public static final int SETTLE_FREEZING = 1;
    /** 资金状态：已冻结 */
    public static final int SETTLE_FROZEN = 2;
    /** 资金状态：结算中 */
    public static final int SETTLE_SETTLING = 3;
    /** 资金状态：已结算 */
    public static final int SETTLE_SETTLED = 4;
    /** 资金状态：退款中 */
    public static final int SETTLE_REFUNDING = 5;
    /** 资金状态：已退回 */
    public static final int SETTLE_REFUNDED = 6;

    /** 操作方：系统 */
    public static final int OPERATOR_SYSTEM = 1;
    /** 操作方：买家 */
    public static final int OPERATOR_BUYER = 2;
    /** 操作方：卖家 */
    public static final int OPERATOR_SELLER = 3;
    /** 操作方：平台 */
    public static final int OPERATOR_PLATFORM = 4;
    /** 操作方：管理员 */
    public static final int OPERATOR_ADMIN = 5;

    private Long id;
    private Long distOrderId;
    private Integer category;
    private String action;
    private Integer fromStatus;
    private Integer toStatus;

    private Long sellerId;
    private String sellerName;

    private Long accountId;
    private Long amount;
    private Integer settleStatus;

    private Integer afterSalesStatus;

    private Integer operatorType;
    private Long operatorId;

    private String remark;
    private Date createTime;

    /** 操作人手机号（查询时补充，非数据库字段） */
    private String operatorPhone;
}
