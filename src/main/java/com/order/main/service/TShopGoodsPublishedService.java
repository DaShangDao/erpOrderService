package com.order.main.service;


import com.order.main.dto.TShopGoodsPublishedDto;
import com.order.main.entity.ErpGoodsOrder;

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
     * 推送订单
     */
    void createSalesOrder(String orderId,String orderSn,String productId,String unitPrice,
                          String quantity,String sales_person,
                          String sales_person_id,String about_id,String shopType,String receiverName,String receiverPhone,String receiverAddress);
}