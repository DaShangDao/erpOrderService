package com.order.main.service;

import com.order.main.entity.Shop;
import com.order.main.entity.ShopGoodsPublished;

/**
 * 修改平台商品库存接口
 */
public interface IEditStockService {


    /**
     * 修改闲鱼平台的商品库存
     * @param shop          店铺信息
     * @param itemId        商品id
     * @param number        库存数量
     * @param type          类型  1 增加库存  2 减少库存
     */
    void xyEditStock(Shop shop, String itemId, String number);

    /**
     * 修改孔夫子平台的商品库存
     * @param token         店铺token
     * @param itemId        商品id
     * @param number        库存数量
     */
    void kfzEditStock(String token,String itemId,String number);

    /**
     * 拼多多修改库存
     * @param shop      店铺信息
     * @param goodsId   商品id
     * @param quantity  库存数量
     */
    void pddEditStock(Shop shop,String goodsId,String quantity);
}
