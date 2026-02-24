package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.ShopGoodsPublished;
import com.order.main.mapper.ShopGoodsPublishedMapper;
import com.order.main.service.IShopGoodsPublishedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记录已发布书籍Service业务层处理
 *
 * @author yxy
 * @date 2025-04-11
 */
@RequiredArgsConstructor
@Service
public class ShopGoodsPublishedServiceImpl implements IShopGoodsPublishedService {


    private final ShopGoodsPublishedMapper baseMapper;
    ;

    /**
     * 根据店铺id查询已发布商品记录
     * @param shopId
     * @return
     */
    @Override
    @DS("slave")
    public List<String> selectListByShopsId(String shopId) {
        return baseMapper.selectListByShopsId(shopId);
    }

    /**
     * 根据平台商品id查询已发布记录
     * @param platformId
     * @return
     */
    @Override
    @DS("slave")
    public List<ShopGoodsPublished> selectByPlatformId(String platformId){
        return baseMapper.selectByPlatformId(platformId);
    }

    /**
     * 根据ERP商品id查询已发布商品记录
     * @param shopGoodsId
     * @return
     */
    @Override
    @DS("slave")
    public List<ShopGoodsPublished> selectByShopGoodsId(String shopGoodsId){
        return baseMapper.selectByShopGoodsId(shopGoodsId);
    }

    /**
     * 根据店铺id和平台商品id查询已发布商品记录
     * @param shopId
     * @param goodsId
     * @return
     */
    @Override
    @DS("slave")
    public ShopGoodsPublished selectByShopIdAndGoodsId(String shopId, String goodsId){
        return baseMapper.selectByShopIdAndGoodsId(shopId,goodsId);
    }

    /**
     * 根据店铺id和平台商品id查询已发布商品记录
     * @param shopId
     * @param platformId
     * @return
     */
    @Override
    @DS("slave")
    public ShopGoodsPublished selectByShopIdAndPlatformId(String shopId, String platformId){
        System.out.println("查询已发布商品参数：shopId:"+shopId+";goodsId:"+platformId);
        return baseMapper.selectByShopIdAndPlatformId(shopId,platformId);
    }

}
