package com.order.main.entity;

import com.order.main.dto.GoodsDto;
import lombok.Data;

/**
 * 平台订单对象 erp_goods_order
 *
 * @author yxy
 * @date 2025-12-04
 */
@Data
public class ErpGoodsOrder {

    /**
     * 
     */
    private Long id;

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
     * 订单中商品sku列表
     */
    private GoodsDto goodsDto;

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
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间  时间戳
     */
    private Long createdAt;

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
     * 收货人
     */
    private String receiverName;

    /**
     * 收货人手机
     */
    private String mobile;

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
     * 修改前的售后状态
     */
    private Long oldAfterSalesStatus;

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
     * erp的订单售后状态  0 无售后  1 待仓库处理  2  待仓库收货 3 售后完成  4 拒绝  5 延后处理 6 再次申请售后
     */
    private Long erpAfterSalesStatus;

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

    // 开始时间
    private Long startTime;
    // 结束时间
    private Long endTime;

}
