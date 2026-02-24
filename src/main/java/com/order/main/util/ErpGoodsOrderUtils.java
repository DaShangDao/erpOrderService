package com.order.main.util;

import com.order.main.entity.ErpGoodsOrder;
import com.order.main.dto.GoodsDto;

public class ErpGoodsOrderUtils {

    /**
     * 创建并设置ErpGoodsOrder对象，参数不为空则set
     */
    public static ErpGoodsOrder createErpGoodsOrder(
            Long id,                                   // 订单ID
            String orderSn,                            // 订单编号
            String shopId,                             // 平台店铺id
            Long shopErpId,                            // erp店铺id
            Long shopType,                             // 店铺类型
            Long catId1,                               // 商品一级id
            Long catId2,                               // 商品二级id
            Long catId3,                               // 商品三级id
            Long catId4,                               // 商品四级id
            String itemList,                           // 订单中商品sku列表json字符串
            GoodsDto goodsDto,                         // 订单中商品sku列表对象
            Long orderTotal,                           // 订单金额（单位：分）
            Long goodsAmount,                          // 商品金额（以分为单位）商品金额=商品销售价格*商品数量-订单改价折扣金额
            Long orderChangeAmount,                    // 订单改价折扣金额（以分为单位）
            Long payAmount,                            // 支付金额（以分为单位）支付金额=商品金额-折扣金额+邮费+服务费
            Long platformDiscount,                     // 平台优惠金额（以分为单位）
            Long createdAt,                            // 创建时间（时间戳）
            Long confirmAt,                            // 成交时间（时间戳）
            Long payAt,                                // 支付时间（时间戳）
            Long shippingAt,                           // 发货时间（时间戳）
            Long receiveAt,                            // 确认收货时间（时间戳）
            Long updatedAt,                            // 订单最近一次更新时间（时间戳）
            Long confirmStatus,                        // 成交状态
            Long orderStatus,                          // 发货状态 1:待付款 2：待发货，3：已发货待签收，4：交易成功 5：已退款 6：交易关闭
            Long oldOrderStatus,                       // 修改用，修改前的订单状态
            Long afterSalesStatus,                     // 售后状态
            String payNo,                              // 支付单号
            Long payType,                              // 支付方式
            Long shippingId,                           // 物流id
            String trackingNumber,                     // 快递单号
            String city,                               // 收件地城市
            Long cityId,                               // 城市编码
            String province,                           // 收件地省份
            Long provinceId,                           // 省份编码
            String country,                            // 收件地国家或地区
            Long countryId,                            // 国家或地区编码
            String town,                               // 收件地区县
            Long townId,                               // 区县编码
            String buyerMemo,                          // 买家留言信息
            String remark,                             // 商家订单备注
            Long remarkTag,                            // 订单备注标记，1-红色，2-黄色，3-绿色，4-蓝色，5-紫色
            String remarkTagName,                      // 订单备注标记名称
            Long isShow,                               // 是否可视化
            String shopMd5Prefix,                      // shop_id的MD5首字母（大写）用于分区
            Long isIssue,                              // 是否下发订单  0 否 1 是
            Integer pageNum,                           // 第几页（分页查询属性）
            Integer pageSize                           // 一页多少条（分页查询属性）
    ) {
        ErpGoodsOrder order = new ErpGoodsOrder();

        // 依次设置所有不为空的参数
        if (id != null) order.setId(id);                                  // 设置订单ID
        if (orderSn != null) order.setOrderSn(orderSn);                   // 设置订单编号
        if (shopId != null) order.setShopId(shopId);                      // 设置平台店铺id
        if (shopErpId != null) order.setShopErpId(shopErpId);             // 设置erp店铺id
        if (shopType != null) order.setShopType(shopType);                // 设置店铺类型
        if (catId1 != null) order.setCatId1(catId1);                      // 设置商品一级id
        if (catId2 != null) order.setCatId2(catId2);                      // 设置商品二级id
        if (catId3 != null) order.setCatId3(catId3);                      // 设置商品三级id
        if (catId4 != null) order.setCatId4(catId4);                      // 设置商品四级id
        if (itemList != null) order.setItemList(itemList);                // 设置订单中商品sku列表json字符串
        if (goodsDto != null) order.setGoodsDto(goodsDto);                // 设置订单中商品sku列表对象
        if (orderTotal != null) order.setOrderTotal(orderTotal);          // 设置订单金额（单位：分）
        if (goodsAmount != null) order.setGoodsAmount(goodsAmount);       // 设置商品金额（以分为单位）
        if (orderChangeAmount != null) order.setOrderChangeAmount(orderChangeAmount); // 设置订单改价折扣金额（以分为单位）
        if (payAmount != null) order.setPayAmount(payAmount);             // 设置支付金额（以分为单位）
        if (platformDiscount != null) order.setPlatformDiscount(platformDiscount); // 设置平台优惠金额（以分为单位）
        if (createdAt != null) order.setCreatedAt(createdAt);             // 设置创建时间（时间戳）
        if (confirmAt != null) order.setConfirmAt(confirmAt);             // 设置成交时间（时间戳）
        if (payAt != null) order.setPayAt(payAt);                         // 设置支付时间（时间戳）
        if (shippingAt != null) order.setShippingAt(shippingAt);          // 设置发货时间（时间戳）
        if (receiveAt != null) order.setReceiveAt(receiveAt);             // 设置确认收货时间（时间戳）
        if (updatedAt != null) order.setUpdatedAt(updatedAt);             // 设置订单最近一次更新时间（时间戳）
        if (confirmStatus != null) order.setConfirmStatus(confirmStatus); // 设置成交状态
        if (orderStatus != null) order.setOrderStatus(orderStatus);       // 设置发货状态
        if (oldOrderStatus != null) order.setOldOrderStatus(oldOrderStatus); // 设置修改前的订单状态
        if (afterSalesStatus != null) order.setAfterSalesStatus(afterSalesStatus); // 设置售后状态
        if (payNo != null) order.setPayNo(payNo);                         // 设置支付单号
        if (payType != null) order.setPayType(payType);                   // 设置支付方式
        if (shippingId != null) order.setShippingId(shippingId);          // 设置物流id
        if (trackingNumber != null) order.setTrackingNumber(trackingNumber); // 设置快递单号
        if (city != null) order.setCity(city);                            // 设置收件地城市
        if (cityId != null) order.setCityId(cityId);                      // 设置城市编码
        if (province != null) order.setProvince(province);                // 设置收件地省份
        if (provinceId != null) order.setProvinceId(provinceId);          // 设置省份编码
        if (country != null) order.setCountry(country);                   // 设置收件地国家或地区
        if (countryId != null) order.setCountryId(countryId);             // 设置国家或地区编码
        if (town != null) order.setTown(town);                            // 设置收件地区县
        if (townId != null) order.setTownId(townId);                      // 设置区县编码
        if (buyerMemo != null) order.setBuyerMemo(buyerMemo);             // 设置买家留言信息
        if (remark != null) order.setRemark(remark);                      // 设置商家订单备注
        if (remarkTag != null) order.setRemarkTag(remarkTag);             // 设置订单备注标记
        if (remarkTagName != null) order.setRemarkTagName(remarkTagName); // 设置订单备注标记名称
        if (isShow != null) order.setIsShow(isShow);                      // 设置是否可视化
        if (shopMd5Prefix != null) order.setShopMd5Prefix(shopMd5Prefix); // 设置shop_id的MD5首字母（大写）用于分区
        if (isIssue != null) order.setIsIssue(isIssue);                   // 设置是否下发订单
        if (pageNum != null) order.setPageNum(pageNum);                   // 设置第几页（分页查询属性）
        if (pageSize != null) order.setPageSize(pageSize);                // 设置一页多少条（分页查询属性）

        return order;
    }
}