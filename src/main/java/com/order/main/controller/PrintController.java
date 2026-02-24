package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.OrderExternalGoods;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IOrderExternalGoodsService;
import com.order.main.service.IZhishuShopGoodsService;
import com.order.main.util.PrintUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 打单
 *
 * @author yxy
 * @date 2026-1-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/print")
public class PrintController {

    private final IErpGoodsOrderService erpGoodsOrderService;
    private final IOrderExternalGoodsService orderExternalGoodsService;
    private final IZhishuShopGoodsService zhishuShopGoodsService;

    /**
     * 创建运单
     * @param orderId       订单id
     * @param type          快递类型
     * @return
     */
    @GetMapping("/createOrder")
    public String createOrder(String orderId,String type){
        // 获取订单信息
        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(Long.parseLong(orderId));
        // 获取订单的商品
        OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(erpGoodsOrder.getId());
        // 获取仓库运费模板
        Map logisticsMap = zhishuShopGoodsService.selectLogisticsByGoodsId(orderExternalGoods.getGoodsId().toString());
        // 联系人/发货人
        String senderName = logisticsMap.get("contact").toString();
        // 联系电话
        String phoneNumber = logisticsMap.get("phone_number").toString();
        // 发货省
        String deliveryProvince = logisticsMap.get("delivery_province").toString();
        // 发货市
        String deliveryCity = logisticsMap.get("delivery_city").toString();
        // 发货区
        String deliveryArea = logisticsMap.get("delivery_area").toString();


        if (type.equals("yunda")){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("appid", PrintUtils.getYundaAppKey());
            jsonObject.put("partner_id","");
            jsonObject.put("secret","");

            List<Object> orders = new ArrayList<>();
            JSONObject order = new JSONObject();
            // 订单唯一序列号 由字母、数字、下划线组成，必须保证唯一，请对特殊符号进行过滤
            order.put("order_serial_no",erpGoodsOrder.getId()+erpGoodsOrder.getOrderSn());
            // 大客户系统订单的订单号可与订单唯一序列号相同
            order.put("khddh",erpGoodsOrder.getId()+erpGoodsOrder.getOrderSn());
            // 发件人对象
            JSONObject sender = new JSONObject();
            // 发件人姓名
            sender.put("name",senderName);
            // 详细地址 必须添加省市区并以半角逗号隔开
            sender.put("address",deliveryProvince+","+deliveryCity+","+deliveryArea);
            // 手机号
            sender.put("mobile",phoneNumber);
            order.put("sender",sender);
            // 收件人对象
            JSONObject receiver = new JSONObject();
            // 收件人姓名
            receiver.put("name","");
            // 详细地址 必须添加省市区并以半角逗号隔开
            receiver.put("address","");
            // 手机号
            receiver.put("mobile","");
            order.put("receiver",receiver);
            // 货物金额
            order.put("value","");
            // 商品信息集合
            List<Object> items = new ArrayList<>();
            // 单个商品列表
            JSONObject item = new JSONObject();
            // 商品名称
            item.put("name","");
            // 商品数量
            item.put("number","");
            // 运单类型，可固定为common
            order.put("order_type","common");
            // 350（默认）
            order.put("node_id","350");
            orders.add(order);
            jsonObject.put("orders",orders);


        }


        return "";

    }


}
