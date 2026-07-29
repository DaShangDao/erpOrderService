package com.order.main.service;


import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;

import java.util.List;

public interface IStoPrintService {


    /**
     * 面单库存查询
     * @param siteCode  网点编号
     * @param userCode  客户编号
     * @param password  客户密码
     * @return
     */
    String getOrderStock(String siteCode,String userCode,String password);


    /**
     * 创建订单
     * @param erpGoodsOrder     订单信息
     * @param receiver          收件人
     * @param sender            寄件人
     * @param items             商品列表
     * @param siteCode          网点编号
     * @param userCode          客户编号
     * @param password          客户密码
     * @return
     */
    String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items, String siteCode, String userCode,String password);


    /**
     * 运单号
     * @param billCode
     * @return
     */
    String orderCancel(String billCode);

}
