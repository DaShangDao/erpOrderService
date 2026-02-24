package com.order.main.dto;

import lombok.Data;

@Data
public class OrderExternalGoodsDto {

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

    /**
     *  erp订单id
     */
    private Long erpOrderId;

    /**
     * 订单编号
     */
    private String orderSn;

    /**
     * 平台店铺id
     */
    private String shopId;

    /**
     * erp店铺id
     */
    private Long shopErpId;

    /**
     * erp店铺名称
     */
    private String shopErpName;

    /**
     * 店铺类型
     */
    private Long shopType;

    /**
     * 商品一级id
     */
    private Long catId1;

    /**
     * 商品二级id
     */
    private Long catId2;

    /**
     * 商品三级id
     */
    private Long catId3;

    /**
     * 商品四级id
     */
    private Long catId4;

    /**
     * 订单中商品sku列表json字符富川
     */
    private String itemList;

    /**
     * 订单金额  单位：分
     */
    private Long orderTotal;

    /**
     * 商品金额（以分为单位）商品金额=商品销售价格*商品数量-订单改价折扣金额
     */
    private Long goodsAmount;

    /**
     * 订单改价折扣金额(以分为单位)
     */
    private Long orderChangeAmount;

    /**
     * 支付金额 (以分为单位) 支付金额=商品金额-折扣金额+邮费+服务费
     */
    private Long payAmount;

    /**
     * 创建时间  时间戳
     */
    private Long erpOrderCreatedAt;

    /**
     * 成交状态
     */
    private Long confirmStatus;

    /**
     * 成交时间 时间戳
     */
    private Long confirmAt;

    /**
     * 支付单号
     */
    private String payNo;

    /**
     * 支付时间  时间戳
     */
    private Long payAt;

    /**
     * 支付方式
     */
    private Long payType;

    /**
     * 平台优惠金额 (以分为单位)
     */
    private Long platformDiscount;

    /**
     * 买家留言信息
     */
    private String buyerMemo;

    /**
     * 发货状态 1: 待付款 2：待发货，3：已发货待签收，4：交易成功 5： 已退款 6 ：交易关闭
     */
    private Long orderStatus;

    /**
     * 修改用，修改前的订单状态
     */
    private Long oldOrderStatus;

    /**
     * 物流id
     */
    private Long shippingId;

    /**
     * 发货时间 时间戳
     */
    private Long shippingAt;

    /**
     * 收件地城市
     */
    private String city;

    /**
     * 城市编码
     */
    private Long cityId;

    /**
     * 收件地省份
     */
    private String province;

    /**
     * 省份编码
     */
    private Long provinceId;

    /**
     * 收件地国家或地区
     */
    private String country;

    /**
     * 国家或地区编码
     */
    private Long countryId;

    /**
     * 收件地区县
     */
    private String town;

    /**
     * 区县编码
     */
    private Long townId;

    /**
     * 快递单号
     */
    private String trackingNumber;

    /**
     * 商家订单备注
     */
    private String remark;

    /**
     * 订单备注标记，1-红色，2-黄色，3-绿色，4-蓝色，5-紫色
     */
    private Long remarkTag;

    /**
     * 订单备注标记名称
     */
    private String remarkTagName;

    /**
     * 确认收货时间
     */
    private Long receiveAt;

    /**
     * 售后状态
     */
    private Long afterSalesStatus;

    /**
     * 订单最近一次更新时间
     */
    private Long updatedAt;

    /**
     * 是否可视化
     */
    private Long isShow;

    /**
     * shop_id的MD5首字母（大写）用于分区
     */
    private String shopMd5Prefix;

    /**
     * 是否下发订单  0 否 1 是
     */
    private Long isIssue;

    /**
     * erp的订单售后状态  0 无售后  1 待仓库处理  2  待仓库收货 3 售后完成  4 拒绝  5 延后处理
     */
    private int erpAfterSalesStatus;

    /**
     * erp订单售后发起时间
     */
    private Long erpAssCreateAt;

    /**
     * 售后原因
     */
    private String erpAssReason;

    /**
     * 拒绝原因
     */
    private String erpAssRemark;

    /**
     * 退货地址
     */
    private String erpAssAddress;

    /**
     * 分页查询属性
     */
    //第几页
    private Integer pageNum;
    //一页多少条
    private Integer pageSize;


    private Long startTime;
    private Long endTime;
}
