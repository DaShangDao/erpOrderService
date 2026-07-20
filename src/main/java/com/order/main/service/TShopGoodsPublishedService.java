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
     * @return
     */
    String synchronizeStockNew(String productId,Long userId,int inventory,int oldInventory,ErpGoodsOrder erpGoodsOrder);


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
     * 孔夫子订单退货事件
     * @param erpGoodsOrder
     */
    void orderReturnhKfz(ErpGoodsOrder erpGoodsOrder);

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

    /**
     * 创建PSI库存同步日志记录（save），返回日志ID
     */
    String savePsiSyncLog(String productId, String productUserId,
                          String erpOrderJson, String platform,
                          String updateType, String shopCreateBy);

    /**
     * 更新PSI库存同步日志（update），设置最终结果
     */
    void updatePsiSyncLog(String id, String shopCreateBy, String quantity,
                          String inventory, String inventoryOld,
                          String code, String msg);

    /**
     * 发布商品到 t_shop_goods_published（先删后插，按库存数量插入多条）
     */
    void publishGoods(Long userId, String productIdStr, String trilateralIdStr,
                      String stockStr, Long shopErpId);
}