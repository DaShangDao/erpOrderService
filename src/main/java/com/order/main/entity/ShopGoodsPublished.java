package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * 记录发布数据对象 t_shop_goods_published
 *
 * @author yxy
 */
@Data
public class ShopGoodsPublished {

    /**
     * 主键
     */
    private Long id;

    /**
     * 图书主键
     */
    private String shopGoodsId;


    /**
     * 发布的店铺ids
     */
    private String shopId;

    /**
     * 平台商品id
     */
    private String platformId;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 商品信息字段
     */
    //isbn
    private String isbn;
    //货号
    private String artNo;
    //价格 单位 分
    private Long price;
    //库存
    private Integer inventory;
    /**
     * 店铺信息字段
     */
    //平台店铺id  / 如果是闲鱼，则是闲管家id
    private String mallId;
    //闲鱼是 用户名
    private String shopKey;
    //店铺类型 1 拼多多  2孔夫子 5闲鱼
    private String shopType;
    //token
    private String token;
    //店铺名称
    private String shopName;
}
