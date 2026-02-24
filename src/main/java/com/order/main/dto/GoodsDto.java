package com.order.main.dto;

import lombok.Data;

import java.util.List;

@Data
public class GoodsDto {

    //商品编号
    private String goodsId;

    //商品数量
    private String goodsCount;

    //商品名称
    private String goodsName;

    //商品销售价格
    private String goodsPrice;

    //商品规格
    private String goodsSpec;

    //商品图片
    private List<String> goodsImgs;

    //商家外部编码（商品）
    private String outerGoodsId;

    //商家外部编码（SKU)  /商品id
    private String outerId;

    //商品规格编码
    private String skuId;
}
