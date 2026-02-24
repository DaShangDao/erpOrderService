package com.order.main.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.Shop;

import java.util.List;


/**
 * 店铺主表Service接口
 *
 * @author yxy
 * @date 2025-03-10
 */
public interface IShopService {

    /**
     * 查询店铺主表
     *
     * @param id 主键
     * @return 店铺主表
     */
    Shop queryById(Long id);

    /**
     * 根据店铺类型查询符合条件的店铺id
     * @param shopType
     * @return
     */
    List<Long> selectShopIdsList(String shopType);

    /**
     * 根据三方平台店铺id查询erp店铺信息
     * @param mallId
     * @return
     */
    Shop selectShopByMallId(String mallId);

    /**
     * 根据shopKey查询erp店铺信息
     * @param shopKey
     * @return
     */
    Shop selectShopByShopKey(String shopKey);

    /**
     * 根据mallId查询店铺群
     * @param shopType
     * @param mallId
     * @return
     */
    List<String> selectShopIdsByMallId(String shopType,String mallId);

    /**
     * 查询开启擦亮的咸鱼店铺
     * @return
     */
    List<Shop> selectOpenPolishShopList();

    /**
     * 修改店铺最后查询订单时间
     * @param shop
     * @return
     */
    int updateShopStartUpdatedAt(Shop shop);
}
