package com.order.main.service;

import java.util.Map;

public interface IPrintSerivce {

    /**
     * 创建订单
     * @param orderId           id
     * @param partnerId         联调账号
     * @param secret            联调密码
     * @param type              快递类型
     * @param cusArea           自定义内容
     * @param deliveryMode      打单类型
     * @param orderSn           订单编号
     * @return
     */
    Map createOrder(String orderId, String partnerId, String secret, String type, String cusArea, String deliveryMode, String orderSn,Map logisticsMap);




    /**
     * 新创建订单
     * @param map
     * @return
     */
    Map createOrderNew(Map map);


    /**
     * 拼多多面单打印
     * @param map
     * @return
     */
    Map createOrderPdd(Map map);

    /**
     * 快递单号取消
     * @param partnerId
     * @param secret
     * @param orderSn
     * @param mailNo
     * @return
     */
    Map cancelBmOrder(String partnerId, String secret, String orderSn,String mailNo);
}
