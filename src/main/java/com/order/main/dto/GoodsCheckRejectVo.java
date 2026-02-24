package com.order.main.dto;


import lombok.Data;

@Data
public class GoodsCheckRejectVo {

    /**
     * 店铺id
     */
    private Long mallId;

    /**
     * 商品id
     */
    private Long goodsId;

    /**
     * 商品草稿id
     */
    private Long goodsCommitId;

    /**
     * 驳回原因
     */
    private String rejectComment;

    /**
     * 驳回时间
     */
    private Long rejectTime;
}
