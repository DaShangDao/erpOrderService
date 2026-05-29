package com.order.main.mapper;

import com.order.main.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 店铺主表Mapper接口
 *
 * @author yxy
 * @date 2025-03-10
 */
@Mapper
public interface ShopMapper {

    Shop selectOneById(Long id);


    /**
     * 批量根据店铺ID集合查询店铺信息
     * @param ids 店铺ID集合
     * @return 店铺列表
     */
    List<Shop> selectBatchByIds(@Param("ids") List<Long> ids);

    /**
     * 根据店铺类型查询符合条件的店铺id
     * @param shopType
     * @return
     */
    List<Long> selectShopIdsList(String shopType);

    /**
     * 根据平台店铺id查询店铺信息
     * @param mallId
     * @return
     */
    Shop selectShopByMallId(String mallId);

    /**
     * 根据shopKey查询店铺信息
     * @param shopKey
     * @return
     */
    Shop selectShopByShopKey(String shopKey);

    /**
     * 根据mallId查询闲鱼店铺群
     * @param shopType
     * @param mallId
     * @return
     */
    List<String> selectShopIdsByMallId(@Param("shopType") String shopType,@Param("mallId") String mallId);

    /**
     * 根据mallId查询闲鱼店铺群
     * @param shopType
     * @param id
     * @return
     */
    String selectShopKeysByMallId(@Param("shopType") String shopType,@Param("id") String id);


    List<Shop> selectOpenPolishShopList();

    /**
     * 修改店铺最后查询订单时间
     * @param shop
     * @return
     */
    int updateShopStartUpdatedAt(Shop shop);
}
