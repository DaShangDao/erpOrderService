package com.order.main.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class XyOrderDto {
    // 闲鱼商品ID
    @JsonProperty("item_id")
    private Long itemID;

    // 订单更新时间
    @JsonProperty("modify_time")
    private Long modifyTime;

    // 闲鱼订单号
    @JsonProperty("order_no")
    private String orderNo;

    // 订单状态
    @JsonProperty("order_status")
    private Long orderStatus;

    // 订单类型 1 普通订单 2 分销订单 3 验货宝订单  4 拍卖订单 7 卡密订单 8 直充订单 9 严选订单 10 特卖订单
    @JsonProperty("order_type")
    private Long orderType;

    // 管家商品ID
    @JsonProperty("product_id")
    private Long productID;

    // 退款状态
    @JsonProperty("refund_status")
    private Long refundStatus;

    // 商家ID
    @JsonProperty("seller_id")
    private Long sellerID;

    // 闲鱼会员名
    @JsonProperty("user_name")
    private String userName;
}