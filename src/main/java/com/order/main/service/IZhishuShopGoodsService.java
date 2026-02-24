package com.order.main.service;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.ZhishuShopGoods;

import java.util.List;
import java.util.Map;

/**
 * 商品信息Service接口

 */
public interface IZhishuShopGoodsService {

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
    ZhishuShopGoods selectById(Long id);

    /**
     * 根据商品id获取运费模板
     * @param goodsId
     * @return
     */
    Map selectLogisticsByGoodsId(String goodsId);

    /**
     * 根据货号查询商品信息
     * @param artNo 货号
     * @return
     */
    ZhishuShopGoods selectByArtNo(String artNo);

    /**
     * 修改库存
     * @param zhishuShopGoods   erp商品信息
     * @param type      1-增加库存 2-减少库存
     */
    int updateInventory(ZhishuShopGoods zhishuShopGoods,String type,Long erpOrderId);

}
