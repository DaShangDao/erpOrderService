package com.order.main.service;

import com.order.main.entity.ShopGoodsPublished;

import java.util.List;

/**
 * 记录已发布书籍Service接口
 *
 * @author yxy
 * @date 2025-04-11
 */
public interface IShopGoodsPublishedService {

    /**
     * 根据店铺id查询已发布商品记录
     * @param shopId
     * @return
     */
    List<String> selectListByShopsId(String shopId);

    /**
     * 根据平台商品id查询已发布记录
     * @param platformId
     * @return
     */
    List<ShopGoodsPublished> selectByPlatformId(String platformId);

    /**
     * 根据ERP商品id查询已发布商品记录
     * @param shopGoodsId
     * @return
     */
    List<ShopGoodsPublished> selectByShopGoodsId(String shopGoodsId);

    /**
     * 根据店铺id和平台商品id查询已发布商品记录
     * @param shopId
     * @param goodsId
     * @return
     */
    ShopGoodsPublished selectByShopIdAndGoodsId(String shopId, String goodsId);

    /**
     * 根据店铺id和平台商品id查询已发布商品记录
     * @param shopId
     * @param platformId
     * @return
     */
    ShopGoodsPublished selectByShopIdAndPlatformId(String shopId, String platformId);
}

