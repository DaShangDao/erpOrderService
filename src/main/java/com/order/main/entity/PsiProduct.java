package com.order.main.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * psi 商品对象
 */
@Data
public class PsiProduct {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 用户id
     */
    private Long aboutId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 标准产品ID
     */
    private Integer standardProductId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 成色/品相
     */
    private Integer appearance;

    /**
     * 条形码
     */
    private String barcode;

    /**
     * 原价/进价（单位：分）
     */
    private Integer price;

    /**
     * 销售价（单位：分）
     */
    private BigDecimal salePrice;

    /**
     * 运费
     */
    private BigDecimal cost;

    /**
     * 是否批次管理：0-否，1-是
     */
    private Integer isBatchManaged;

    /**
     * 是否保质期管理：0-否，1-是
     */
    private Integer isShelfLifeManaged;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间（时间戳）
     */
    private Long createdAt;

    /**
     * 更新时间（时间戳）
     */
    private Long updatedAt;
}