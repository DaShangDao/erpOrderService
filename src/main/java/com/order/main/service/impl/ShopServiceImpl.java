package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.Shop;
import com.order.main.mapper.ShopMapper;
import com.order.main.service.IShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 店铺主表Service业务层处理
 *
 * @author yxy
 * @date 2025-03-10
 */
@RequiredArgsConstructor
@Service
public class ShopServiceImpl implements IShopService {

    private final ShopMapper baseMapper;


    @Override
    @DS("slave")
    public Shop queryById(Long id) {
        return baseMapper.selectOneById(id);
    }

    /**
     * 根据店铺类型查询符合条件的店铺id
     * @param shopType
     * @return
     */
    @Override
    @DS("slave")
    public List<Long> selectShopIdsList(String shopType){
        return baseMapper.selectShopIdsList(shopType);
    }

    @Override
    @DS("slave")
    public Shop selectShopByMallId(String mallId){
        return baseMapper.selectShopByMallId(mallId);
    }

    @Override
    @DS("slave")
    public Shop selectShopByShopKey(String shopKey){
        return baseMapper.selectShopByShopKey(shopKey);
    }

    @Override
    @DS("slave")
    public List<String> selectShopIdsByMallId(String shopType, String mallId) {
        return baseMapper.selectShopIdsByMallId(shopType, mallId);
    }

    @Override
    @DS("slave")
    public List<Shop> selectOpenPolishShopList() {
        return baseMapper.selectOpenPolishShopList();
    }

    /**
     * 修改店铺最后查询订单时间
     * @param shop
     * @return
     */
    @Override
    @DS("slave")
    public int updateShopStartUpdatedAt(Shop shop){
        return baseMapper.updateShopStartUpdatedAt(shop);
    }

}
