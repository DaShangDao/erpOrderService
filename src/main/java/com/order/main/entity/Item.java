package com.order.main.entity;

import lombok.Data;

/**
 * 商品信息
 */
@Data
public class Item {

    /**
     * 物品名称
     */
    private String name;

    /**
     * 物品数量
     */
    private String num;

    /**
     * isbn
     */
    private String isbn;

    /**
     * 货号
     */
    private String artNo;

    /**
     * 原货号
     */
    private String originalArtNo;
}
