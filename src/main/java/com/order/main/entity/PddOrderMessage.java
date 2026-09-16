package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * 拼多多订单类/备注类消息队列实体 t_pdd_order_message
 *
 * @author yxy
 * @date 2026-08-04
 */
@Data
public class PddOrderMessage {

    /**
     * 主键ID（自增）
     */
    private Long id;

    /**
     * 消息类型，如 pdd_trade_TradeConfirmed
     */
    private String msgType;

    /**
     * 店铺MallID
     */
    private Long mallId;

    /**
     * ERP店铺ID（t_shop.id），入库时查询缓存得到
     */
    private Long shopId;

    /**
     * 消息原始content(JSON)
     */
    private String content;

    /**
     * 状态：0=待处理 1=处理中 2=成功 3=失败(待重试) 9=终态(重试后仍失败)
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
