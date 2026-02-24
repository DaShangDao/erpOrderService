package com.order.main.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品信息对象 zhishu_shop_goods
 *
 * @author yxy
 */
@Data
public class ZhishuShopGoods  {

    /**
     * id
     */
    private String id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 产品编码
     */
    private String productId;

    /**
     * 是否加入分销：0-否 1-是
     */
    private Integer isJoinDistribution;

    /**
     * 仓库Id
     */
    private Long depotId;

    /**
     * 仓库的创建人
     */
    private Long depotUserId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * isbn
     */
    private String isbn;

    /**
     * 货号
     */
    private String artNo;

    /**
     * 原始货号
     */
    private String originalArtNo;

    /**
     * 期初库存
     */
    private Long stock;

    /**
     * 标准售价
     */
    private Long price;

    /**
     * 品相
     */
    private String conditionCode;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;

    /**
     * 商品编号
     */
    private String itemNumber;

    /**
     * 商品定价
     */
    private Long fixPrice;

    /**
     * 库存
     */
    private Long inventory;

    /**
     * 修改前库存
     */
    private Long oldInventory;

    /**
     * 图片
     */
    private String bookPic;

    /**
     * 是否已进行货号转换：0-未转换 1-已转换
     */
    private Integer isArtNoConversion;

    /**
     * 图书分类ID
     */
    private Long categoryId;
    /**
     * 模板类型
     */
    private String templateType;
    /**
     * 作者
     */
    private String author;
    /**
     * 出版社
     */
    private String publisher;
    /**
     * 纸张
     */
    private String format;
    /**
     * 打印时间
     */
    private String printTime;
    /**
     * 字数
     */
    private String wordage;
    /**
     * 统一书号
     */
    private String goodUnifyIsbn;

    /**
     * 运费
     */
    private String templateMinPrice;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 自营书品的运费
     */
    private BigDecimal shippingCost;

    /**
     * 总费用
     */
    private BigDecimal totalCost;

    /**
     * 手续费
     */
    private BigDecimal serviceCharge;

    /**
     * 发货地
     */
    private String placeOfDispatch;

    /**
     * 分页参数
     */
    private int pageNum;
    private int pageSize;
}

