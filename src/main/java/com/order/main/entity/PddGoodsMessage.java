package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * 拼多多商品类消息中转实体 t_pdd_goods_message
 * 仅做中转：定时器查出后先删除，再存入Redis，不保留状态
 *
 * @author yxy
 * @date 2026-08-04
 */
@Data
public class PddGoodsMessage {

    /**
     * 主键ID（自增）
     */
    private Long id;

    /**
     * 消息类型，如 pdd_goods_GoodsOffShelf
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
     * 创建时间
     */
    private Date createTime;
}
