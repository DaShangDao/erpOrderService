package com.order.main.dto;

import lombok.Data;

/**
 * 已发布商品信息DTO
 */
@Data
public class TShopGoodsPublishedDto {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * erp店铺id
     */
    private Long erpShopId;

    /**
     * 平台店铺名称
     */
    private String shopName;

    /**
     * 店铺名称
     */
    private String erpShopName;

    /**
     * 新商品id
     */
    private Long productId;

    /**
     * 新仓库id
     */
    private Long warehouseId;

    /**
     * 平台店铺id
     */
    private Long shopId;

    /**
     * 平台商品id
     */
    private Long trilateralId;

    /**
     * 商品持有人id
     */
    private Long userId;

    private int delFlag;

    private Long createTime;

    private Long updateTime;

    // 是否是分销商品  1 是
    private String isdistribution;

    // ==================== 分页参数 ====================

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;
}