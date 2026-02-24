package com.order.main.mapper;

import com.order.main.entity.ShopGoodsPublished;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 记录已发布书籍Mapper接口
 *
 * @author yxy
 * @date 2025-04-11
 */
@Mapper
public interface ShopGoodsPublishedMapper {

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
     * 根据店铺id和ERP商品id查询已发布商品记录
     * @param shopId
     * @param goodsId
     * @return
     */
    ShopGoodsPublished selectByShopIdAndGoodsId(@Param("shopId")String shopId, @Param("goodsId") String goodsId);

    /**
     * 根据店铺id和平台商品id查询已发布商品记录
     * @param shopId
     * @param platformId
     * @return
     */
    ShopGoodsPublished selectByShopIdAndPlatformId(@Param("shopId")String shopId, @Param("platformId") String platformId);
}
