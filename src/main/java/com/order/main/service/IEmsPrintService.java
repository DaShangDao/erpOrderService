package com.order.main.service;

import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;

import java.util.List;

public interface IEmsPrintService {


    /**
     *  面单查询
     * @param authorization         协议客户号
     * @param waybillNo             快递单
     * @param type                  面单类型 129：总部模板76129    149：总部模板76149      179：总部模板100179
     * @return
     */
    String getOrder(String authorization,String waybillNo,String type);


    /**
     * 创建订单
     * @param erpGoodsOrder     订单信息
     * @param receiver          收件人
     * @param sender            寄件人
     * @param items             商品列表
     * @param senderNo     协议客户号
     * @return
     */
    String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items,String senderNo,String authorization,String secretKey);


    /**
     * 快递单取消
     * @param logisticsOrderNo      订单号
     * @param waybillNo             快递号
     * @param cancelReason          取消原因
     * @param senderNo              协议客户号
     * @return
     */
    String cancelBmOrder(String logisticsOrderNo,String waybillNo,String cancelReason,String senderNo,String authorization,String secretKey);
}
