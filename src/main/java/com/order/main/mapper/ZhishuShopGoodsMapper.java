package com.order.main.mapper;


import com.order.main.entity.ZhishuShopGoods;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 商品信息Mapper接口
 *
 */
@Mapper
public interface ZhishuShopGoodsMapper  {

    /**
     * 根据查询条件查询单挑数据
     * @param zhishuShopGoods
     * @return
     */
    List<ZhishuShopGoods> selectList(ZhishuShopGoods zhishuShopGoods);

    /**
     * 根据id查询商品信息
     * @param id
     * @return
     */
    ZhishuShopGoods selectById(String id);

    /**
     * 根据商品id获取运费模板
     * @param goodsId
     * @return
     */
    Map selectLogisticsByGoodsId(String goodsId);

    /**
     * 根据货号查询商品信息
     * @param artNo
     * @return
     */
    ZhishuShopGoods selectByArtNo(String artNo);

    /**
     * 修改库存
     * @param zhishuShopGoods   erp商品信息
     */
    int updateInventory(ZhishuShopGoods zhishuShopGoods);


}