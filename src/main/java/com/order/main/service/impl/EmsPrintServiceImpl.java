package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;
import com.order.main.service.IEmsPrintService;
import com.order.main.util.DateUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Service
public class EmsPrintServiceImpl implements IEmsPrintService {


    /**
     * 面单查询
     * @param authorization         协议客户号
     * @param waybillNo             快递单
     * @param type                  面单类型 129：总部模板76129    149：总部模板76149      179：总部模板100179
     * @return
     */
    @Override
    public String getOrder(String authorization, String waybillNo, String type) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("waybillNo",waybillNo);
        jsonObject.put("type",type);
        return PrintSimpleDllLoader.exceteEms("010004","1100213092201",authorization,"Y3pJZ2lySXM0UEZ5YUlueQ==",jsonObject.toString());

    }

    /**
     * 创建订单
     * @param erpGoodsOrder     订单信息
     * @param receiver          收件人
     * @param sender            寄件人
     * @param items             商品列表
     * @param senderNo     协议客户号
     * @return
     */
    @Override
    public String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items,String senderNo,String authorization,String secretKey) {
        List<JSONObject> orderList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("createdTime", DateUtils.getTimeByDayOffset(0));
        // 电商客户标识
        jsonObject.put("ecommerceUserId",erpGoodsOrder.getCreatedBy());
        // 物流订单号
        jsonObject.put("logisticsOrderNo",erpGoodsOrder.getOrderSn());
        // 一票多件标志
        jsonObject.put("oneBillFlag","0");
        // 内件性质 1：文件 3、物品 2：信函 4、包裹
        jsonObject.put("contentsAttribute","3");
        // 业务产品分类 1.特快专递  2.快递包裹 3.到付 9.即日 10.电商标快 11.标准快递
        jsonObject.put("bizProductNo","2");
        // 备注
        jsonObject.put("pickupNotes","");
        // 付款方式 1:寄件人 2:收件人 3:第三方 4:收件人集中付费 5:免费 6:寄/收件人 7:预付卡
        jsonObject.put("paymentMode","1");
        /**
         * 寄件人
         */
        JSONObject senderData = new JSONObject();
        senderData.put("name",sender.getName());
        senderData.put("phone",sender.getPhone());
        senderData.put("mobile",sender.getMobile());
        senderData.put("prov",sender.getProv());
        senderData.put("city",sender.getCity());
        senderData.put("county",sender.getCounty());
        senderData.put("address",sender.getAddress());
        jsonObject.put("sender",senderData);
        /**
         * 收件人
         */
        JSONObject receiverData = new JSONObject();
        receiverData.put("name",receiver.getName());
        receiverData.put("phone",receiver.getPhone());
        receiverData.put("mobile",receiver.getMobile());
        receiverData.put("prov",receiver.getProv());
        receiverData.put("city",receiver.getCity());
        receiverData.put("county",receiver.getCounty());
        receiverData.put("address",receiver.getAddress());
        jsonObject.put("receiver",receiverData);
        /**
         * 商品信息
         */
        List<JSONObject> cargos = new ArrayList<>();
        for (Item item : items){
            JSONObject cargo = new JSONObject();
            cargo.put("cargoName",item.getName());
            cargo.put("cargoQuantity",item.getNum());
            cargos.add(cargo);
        }
        jsonObject.put("cargos",cargos);
        orderList.add(jsonObject);

        return PrintSimpleDllLoader.exceteEms("020003",senderNo,authorization,secretKey, JsonUtil.transferToJson(orderList));
        /**
         *  logisticsOrderNo        物流订单号（客户内部订单号）
         *  waybillNo               物流运单号（一票多件、返单业务单号逗号分隔）
         *  routeCode               四段码（分拣码）
         *  markDestinationCode     大头笔编码
         *  markDestinationName     大头笔
         *  packageCode             集包地编码
         *  packageCodeName         集包地名称
         */
    }


    /**
     * 快递单取消
     * @param logisticsOrderNo      订单号
     * @param waybillNo             快递号
     * @param cancelReason          取消原因
     * @param senderNo         协议客户号
     * @return
     */
    @Override
    public String cancelBmOrder(String logisticsOrderNo,String waybillNo,String cancelReason,String senderNo,String authorization,String secretKey){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("logisticsOrderNo",logisticsOrderNo);
        jsonObject.put("waybillNo",waybillNo);
        jsonObject.put("cancelReason",cancelReason);
        return PrintSimpleDllLoader.exceteEms("020006",senderNo,authorization,secretKey,jsonObject.toString());
    }
}
