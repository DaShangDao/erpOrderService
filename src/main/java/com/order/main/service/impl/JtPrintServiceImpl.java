package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.*;
import com.order.main.service.ICourierLogService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IJtPrintService;
import com.order.main.util.DateUtils;
import com.order.main.util.SignatureUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class JtPrintServiceImpl implements IJtPrintService {


    private final IErpGoodsOrderService erpGoodsOrderService;
    private final ICourierLogService courierLogService;

    /**
     * 电子面单账号校验
     * @param customerCode  账号
     * @param password      密码
     * @return
     */
    @Override
    public String jtVipCheckCusPwd(String customerCode,String password) {
        // 参数定义
        JSONObject params = new JSONObject();
        params.put("customerCode",customerCode);
        params.put("digest", SignatureUtils.generateSignature(customerCode,password,"4e5aba3a245e4d9a8e18055c187dc9c1"));
        return PrintSimpleDllLoader.execteJtApi("JtVipCheckCusPwd","814346259086152064","4e5aba3a245e4d9a8e18055c187dc9c1",params.toString());
    }


    /**
     * 电子面单账号余额查询
     * @param customerCode
     * @param password
     * @return
     */
    @Override
    public String jtEssBalance(String customerCode,String password) {
        // 参数定义
        JSONObject params = new JSONObject();
        params.put("customerCode",customerCode);
        params.put("digest", SignatureUtils.generateSignature(customerCode,password,"4e5aba3a245e4d9a8e18055c187dc9c1"));
        return PrintSimpleDllLoader.execteJtApi("JtEssBalance","814346259086152064","4e5aba3a245e4d9a8e18055c187dc9c1",params.toString());
    }


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
    @Override
    public String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items,String customerCode,String password) {
        JSONObject params = new JSONObject();
        params.put("customerCode",customerCode);
        params.put("digest", SignatureUtils.generateSignature(customerCode,password,"4e5aba3a245e4d9a8e18055c187dc9c1"));
        // 客户订单号
        params.put("txlogisticId",erpGoodsOrder.getOrderSn());
        //快件类型：EZ(标准快递) TYD(兔优达)
        params.put("expressType","EZ");
        // 订单类型（有客户编号为月结）1、 散客；2、月结；
        params.put("orderType","1");
        // 服务类型 ：02 门店寄件 ； 01 上门取件
        params.put("serviceType","01");
        // 派送类型： 06 代收点自提 05 快递柜自提 04 站点自提 03 派送上门
        params.put("deliveryType","06");
        // 支付方式：PP_PM("寄付月结")
        params.put("payType","PP_PM");
        //寄件信息对象
        JSONObject senderObject = new JSONObject();
        // 名称
        senderObject.put("name",sender.getName());
        // 电话
        senderObject.put("phone",sender.getPhone());
        // 座机
        senderObject.put("mobile",sender.getMobile());
        //寄件国家三字码（如：中国=CHN、印尼=IDN）
        senderObject.put("countryCode","CHN");
        // 省
        senderObject.put("prov",sender.getProv());
        // 市
        senderObject.put("city",sender.getCity());
        // 区
        senderObject.put("area",sender.getCounty());
        // 寄件详细地址（省+市+区县+详细地址）
        senderObject.put("address",sender.getProv()+sender.getCity()+sender.getCounty()+sender.getAddress());
        params.put("sender",senderObject);
        // 收件人信息
        JSONObject receiverObject = new JSONObject();
        // 名称
        receiverObject.put("name",receiver.getName());
        // 电话
        receiverObject.put("phone",receiver.getPhone());
        // 座机
        receiverObject.put("mobile",receiver.getMobile());
        //寄件国家三字码（如：中国=CHN、印尼=IDN）
        receiverObject.put("countryCode","CHN");
        // 省
        receiverObject.put("prov",receiver.getProv());
        // 市
        receiverObject.put("city",receiver.getCity());
        // 区
        receiverObject.put("area",receiver.getCounty());
        // 寄件详细地址（省+市+区县+详细地址）
        receiverObject.put("address",receiver.getProv()+receiver.getCity()+receiver.getCounty()+receiver.getAddress());
        params.put("receiver",receiverObject);
        // 物品类型（对应订单主表物品类型）: bm000001 文件 bm000002 数码产品 bm000003 生活用品  bm000004 食品  bm000005 服饰  bm000006 其他 bm000007 生鲜类 bm000008 易碎品 bm000009 液体
        params.put("goodsType","bm000003");
        // 重量，单位kg，范围0.01-30
        params.put("weight","1");
        // 商品信息列表
        List<JSONObject> itemsObject = new ArrayList<>();
        for (Item item : items){
            JSONObject itemObject = new JSONObject();
            // 物品类型（对应订单主表物品类型）: bm000001 文件 bm000002 数码产品 bm000003 生活用品  bm000004 食品  bm000005 服饰  bm000006 其他 bm000007 生鲜类 bm000008 易碎品 bm000009 液体
            itemObject.put("itemType","bm000003 ");
            // 名称
            itemObject.put("itemName",item.getName());
            // 件数，≤1
            itemObject.put("number",item.getNum());
            itemsObject.add(itemObject);
        }
        params.put("items",itemsObject);
        // 创建订单
        return PrintSimpleDllLoader.execteJtApi("jtOrderAddOrder","814346259086152064","4e5aba3a245e4d9a8e18055c187dc9c1",params.toString());
    }


    /**
     * 电子面单取消
     * @param customerCode  客户编码（订单类型传2时，必填）
     * @param txlogisticId  客户订单号    传客户自己系统的订单号
     * @param reason        取消原因
     * @return
     */
    @Override
    public String jtOrderCancelOrder(String customerCode,String password,String txlogisticId,String reason) {
        // 参数定义
        JSONObject params = new JSONObject();
        params.put("customerCode",customerCode);
        params.put("digest", SignatureUtils.generateSignature(customerCode,password,"4e5aba3a245e4d9a8e18055c187dc9c1"));
        params.put("orderType","1");
        params.put("txlogisticId",txlogisticId);
        params.put("reason",reason);
        return PrintSimpleDllLoader.execteJtApi("JtOrderCancelOrder","814346259086152064","4e5aba3a245e4d9a8e18055c187dc9c1",params.toString());
    }

}
