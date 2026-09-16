package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.order.main.entity.Shop;
import com.order.main.service.IShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 店铺信息缓存服务（mallId -> Shop，缓存1天）
 * t_shop 表在 slave 数据源（zhishu库），查询必须走 @DS("slave")
 * 独立Bean：保证 @DS 注解在跨Bean调用时生效（同类内部调用AOP不生效）
 *
 * @author yxy
 * @date 2026-08-04
 */
@Service
@RequiredArgsConstructor
public class ShopCacheService {

    private final IShopService shopService;

    /**
     * mallId -> Shop 缓存，1天过期
     * 缓存存在直接用，不存在则查库并写入缓存
     */
    private final Cache<Long, Shop> shopCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    /**
     * 获取店铺信息（带缓存）
     * 查询SQL：Shop shop = shopService.selectShopByMallId(mallId) （slave库）
     */
    @DS("slave")
    public Shop getShop(Long mallId) {
        Shop shop = shopCache.getIfPresent(mallId);
        if (shop != null) {
            return shop;
        }
        shop = shopService.selectShopByMallId(mallId.toString());
        if (shop != null) {
            shopCache.put(mallId, shop);
        }
        return shop;
    }

    /**
     * 获取shopId（带缓存）
     * 用于入库时记录 shop_id 字段
     */
    public Long getShopId(Long mallId) {
        Shop shop = getShop(mallId);
        return shop == null ? null : shop.getId();
    }
}
