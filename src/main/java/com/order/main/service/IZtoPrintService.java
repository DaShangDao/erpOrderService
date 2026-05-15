package com.order.main.service;

import com.order.main.entity.ErpGoodsOrder;

import java.util.List;
import java.util.Map;

public interface IZtoPrintService {


    /**
     * 电子面单绑定
     * @param account
     * @param password
     * @param dataMap
     * @return
     */
    String bindingEaccount(String account, String password,Map dataMap);

    /**
     * 获取快递余额
     * @param account
     * @param password
     * @param dataMap
     * @return
     */
    String faceSheetBalance(String account, String password, Map dataMap);

    /**
     * 创建订单
     * @param erpGoodsOrderList
     * @param logisticsMap
     * @return
     */
    String createOrder(String accountId,String accountPassword,List<ErpGoodsOrder> erpGoodsOrderList,Map logisticsMap);


    /**
     * 取消订单
     * @param cancelType
     * @param billCode
     * @return
     */
    String cancelPreOrder(String cancelType,String billCode);

    /**
     * 请求生成面单图片/PDF
     * @param billCode
     * @return
     */
    String orderPrint(String billCode);

    /**
     * 查询订单接口
     * @param type
     * @param billCode
     * @return
     */
    String getOrderInfo(String type,String billCode);
}
