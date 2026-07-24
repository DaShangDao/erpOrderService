package com.order.main.service;


import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;

import java.util.List;

/**
 * 圆通
 */
public interface IYtoPrintService {


    /**
     * 创建订单
     * @param erpGoodsOrder     订单信息
     * @param receiver          收件人
     * @param sender            寄件人
     * @param items             商品列表
     * @param customerCode      账号
     * @param secretKey         密码
     * @return
     */
    String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items, String customerCode, String secretKey);


    /**
     * 获取电子面单余额
     * @param customerCode
     * @param secretKey
     * @return
     */
    String getOrderNum(String customerCode, String secretKey);
}
