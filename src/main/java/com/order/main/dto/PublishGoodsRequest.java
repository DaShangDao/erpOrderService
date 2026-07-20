package com.order.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 发布商品到 t_shop_goods_published 的请求参数
 */
@Data
public class PublishGoodsRequest {
    /** 用户ID */
    @JsonProperty("user_id")
    private Long userId;

    /** 商品ID列表（逗号分隔） */
    @JsonProperty("product_id")
    private String productId;

    /** 三方平台商品ID列表（逗号分隔，与productId一一对应） */
    @JsonProperty("trilateral_id")
    private String trilateralId;

    /** 库存列表（逗号分隔，与productId一一对应） */
    private String stock;

    /** ERP店铺ID */
    @JsonProperty("shop_erp_id")
    private Long shopErpId;

    /** 平台店铺ID */
    @JsonProperty("shop_id")
    private String shopId;

    /** 任务ID */
    @JsonProperty("task_id")
    private String taskId;

    /** 订单数据（JSON字符串） */
    @JsonProperty("order_data")
    private String orderData;
}
