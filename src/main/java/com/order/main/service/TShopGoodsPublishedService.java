package com.order.main.service;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.dto.TShopGoodsPublishedDto;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ShopGoodsPublished;
import com.order.main.entity.WarehouseSettings;

import java.util.List;
import java.util.Map;


/**
 * 已发布商品信息Service接口
 */
public interface TShopGoodsPublishedService {

    /**
     * 根据ID查询
     *
     * @param id 主键ID
     * @return 已发布商品信息
     */
    TShopGoodsPublishedDto selectById(Long id);


    /**
     * 根据平台商品id查询
     * @param trilateralId
     * @return
     */
    List<TShopGoodsPublishedDto> selectByTrilateralId(Long trilateralId);

    /**
     * 根据进销存商品id查询
     * @param productId
     * @return
     */
    List<TShopGoodsPublishedDto> selectByProductId(Long productId,Long userId);

    /**
     * 查询一条已被删除的数据
     * @param productId
     * @param userId
     * @param trilateralId
     * @return
     */
    TShopGoodsPublishedDto selectDelFlag(Long productId,Long userId,Long trilateralId);

    int update(Long id);

    int updateShopGoodsPublishedRecover(Long id);

    int deleteById(Long id);


    /**
     * 进销存 库存同步方法
     * @param productId         进销存商品id
     * @param inventory         新库存
     * @param oldInventory      原库存
     * @param erpGoodsId        订单id
     * @return
     */
    String synchronizeStockNew(String productId,Long userId,int inventory,int oldInventory,Long erpGoodsId);


    /**
     * 推送订单
     */
    void createSalesOrder(ErpGoodsOrder erpGoodsOrder, WarehouseSettings warehouseSettings);


    /**
     * 订单完成事件
     */
    void orderFinish(ErpGoodsOrder erpGoodsOrder);


    /**
     * 订单退货事件
     */
    void orderReturnh(ErpGoodsOrder erpGoodsOrder);

    /**
     * 测试
     * @param orderId
     * @param orderSn
     * @param productId
     * @param unitPrice
     * @param quantity
     * @param sales_person
     * @param sales_person_id
     * @param about_id
     * @param shopType
     * @param receiverName
     * @param receiverPhone
     * @param receiverAddress
     */
    void createSalesOrder(String orderId,String orderSn,String productId,String unitPrice,
                          String quantity,String sales_person,
                          String sales_person_id,String about_id,String shopType,String receiverName,String receiverPhone,String receiverAddress);
}