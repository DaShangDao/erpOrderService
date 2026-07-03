package com.order.main.service;


import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;

import java.util.List;

/**
 * 极兔
 */
public interface IJtPrintService {

    /**
     * 电子面单绑定
     * @param customerCode  账号
     * @param password      密码
     * @return
     */
    String jtVipCheckCusPwd(String customerCode,String password);

    /**
     * 电子面单账号余额查询
     * @param customerCode
     * @param password
     * @return
     */
    String jtEssBalance(String customerCode,String password);


    /**
     * 创建商品
     * @param erpGoodsOrder
     * @param receiver
     * @param sender
     * @param items
     * @param customerCode
     * @param password
     * @return
     */
    String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items, String customerCode, String password);

    /**
     * 电子面单取消
     * @param customerCode  客户编码（订单类型传2时，必填）
     * @param txlogisticId  客户订单号    传客户自己系统的订单号
     * @param reason        取消原因
     * @return
     */
    String jtOrderCancelOrder(String customerCode,String password,String txlogisticId,String reason);
}
