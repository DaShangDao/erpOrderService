package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;
import com.order.main.service.IYtoPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class YtoPrintServiceImpl implements IYtoPrintService {


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
    @Override
    public String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items, String customerCode, String secretKey){
        JSONObject jsonObject = new JSONObject();
        // 	物流单号，打印拉取运单号前，物流单号和渠道唯一确定一笔快递物流订单。注：最低长度为7
        jsonObject.put("logisticsNo",erpGoodsOrder.getOrderSn());
        // 	寄件人姓名
        jsonObject.put("senderName",sender.getName());
        // 	寄件人省名称
        jsonObject.put("senderProvinceName",sender.getProv());
        // 	寄件人市名称
        jsonObject.put("senderCityName",sender.getCity());
        // 寄件人区县名称
        jsonObject.put("senderCountyName",sender.getCounty());
        // 	寄件人详细地址
        jsonObject.put("senderAddress",sender.getAddress());
        // 寄件人联系电话
        jsonObject.put("senderMobile",sender.getPhone());
        // 	收件人姓名
        jsonObject.put("recipientName",receiver.getName());
        // 收件人省名称
        jsonObject.put("recipientProvinceName",receiver.getProv());
        // 收件人市名称
        jsonObject.put("recipientCityName",receiver.getCity());
        // 	收件人区县名称
        jsonObject.put("recipientCountyName",receiver.getCounty());
        // 	收件人详细地址
        jsonObject.put("recipientAddress",receiver.getAddress());
        // 收件人联系电话
        jsonObject.put("recipientMobile",receiver.getPhone());

        List<JSONObject> goodsList = new ArrayList<>();
        for (Item item : items){
            JSONObject goods = new JSONObject();
            goods.put("name",item.getName());
            goods.put("quantity",item.getNum());
            goodsList.add(goods);
        }
        jsonObject.put("goods",goodsList);

        return PrintSimpleDllLoader.exceteYTO("privacy_create_adapter",customerCode,secretKey,jsonObject.toString());
    }

}
