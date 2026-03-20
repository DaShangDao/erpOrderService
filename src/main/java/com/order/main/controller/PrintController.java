package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.CourierLog;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.OrderExternalGoods;
import com.order.main.service.ICourierLogService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IOrderExternalGoodsService;
import com.order.main.service.IZhishuShopGoodsService;
import com.order.main.util.PrintUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

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
    private final ICourierLogService courierLogService;

    /**
     * 创建运单
     * @param orderId       订单id
     * @param type          快递类型
     * @return
     */
    @GetMapping("/createOrder")
    public Map createOrder(String orderId,String partnerId,String secret,String type,String cusArea,String deliveryMode,String orderSn){
        // 返回值对象定义
        Map result = new HashMap();


        ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();

        erpGoodsOrder.setOrderStatus(2L);
        if (deliveryMode.equals("1")){
            // 订单全部商品打印
            erpGoodsOrder.setOrderSn(orderSn);
        }else {
            // 订单单独商品打印
            erpGoodsOrder.setId(Long.parseLong(orderId));
        }

        List<ErpGoodsOrder>  erpGoodsOrderList = erpGoodsOrderService.selectOrderList(erpGoodsOrder);

        if (erpGoodsOrderList.isEmpty()){
            result.put("code","500");
            result.put("msg","订单不存在");
            return result;
        }

        // 操作类型
        String operationType = "ADD_ORDER";
        // 发货地对象
        Map logisticsMap = null;
        Iterator<ErpGoodsOrder> iterator = erpGoodsOrderList.iterator();
        while (iterator.hasNext()) {
            ErpGoodsOrder ego = iterator.next();
            // 获取订单的商品
            OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(ego.getId());
            if (orderExternalGoods == null) {
                iterator.remove(); // 安全删除
            }else if (null == logisticsMap){
                logisticsMap = zhishuShopGoodsService.selectLogisticsByGoodsId(orderExternalGoods.getGoodsId().toString());
            }
        }

        if (erpGoodsOrderList.isEmpty()){
            result.put("code","500");
            result.put("msg","订单未获取到指定商品信息");
            return result;
        }

        erpGoodsOrder = erpGoodsOrderList.get(0);
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
            jsonObject.put("appid", "004064");
            jsonObject.put("partner_id",partnerId);
            jsonObject.put("secret",secret);
            List<Object> orders = new ArrayList<>();
            JSONObject order = new JSONObject();
            // 订单唯一序列号 由字母、数字、下划线组成，必须保证唯一，请对特殊符号进行过滤
            order.put("order_serial_no",erpGoodsOrder.getOrderSn());
            // 大客户系统订单的订单号可与订单唯一序列号相同
            order.put("khddh",erpGoodsOrder.getOrderSn());
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
            receiver.put("name",erpGoodsOrder.getReceiverName());
            // 详细地址 必须添加省市区并以半角逗号隔开
            receiver.put("address",erpGoodsOrder.getProvince()+","+erpGoodsOrder.getCity()+","+erpGoodsOrder.getTown());
            // 手机号
            receiver.put("mobile",erpGoodsOrder.getMobile());
            order.put("receiver",receiver);
            // 货物金额
            order.put("value",erpGoodsOrder.getPayAmount());
            // 商品信息集合
            List<Object> items = new ArrayList<>();
            for (ErpGoodsOrder ego : erpGoodsOrderList){
                // 商品信息
                GoodsDto goodsDto = JsonUtil.transferToObj(ego.getItemList(),GoodsDto.class);
                // 单个商品列表
                JSONObject item = new JSONObject();
                // 商品名称
                item.put("name",goodsDto.getGoodsName());
                // 商品数量
                item.put("number",goodsDto.getGoodsCount());
                // 说明
                item.put("remark","");
                items.add(item);
            }
            order.put("items",items);
            // 运单类型，可固定为common
            order.put("order_type","common");
            // 350（默认）
            order.put("node_id","350");
            // 自定义显示信息1
            order.put("cus_area1",cusArea);
            orders.add(order);
            jsonObject.put("orders",orders);
             String res = DllInitializer.ydCreateBmOrder(jsonObject.toString(),"004064","eed7ae222b8541deae79cdfc318b7aa8");

            if(res.contains("更新订单请使用更新接口")){
                res = DllInitializer.ydUpdateBmOrder(jsonObject.toString(),"004064","eed7ae222b8541deae79cdfc318b7aa8");
                operationType = "UPDATE_ORDER";
            }

            Map resMap = JsonUtil.transferToObj(res,Map.class);
            if (resMap.get("code").equals("0000")){

                List dataList = (List) resMap.get("data");
                Map data = (Map) dataList.get(0);

                for (ErpGoodsOrder ego : erpGoodsOrderList){
                    // 日志对象定义
                    CourierLog courierLog = new CourierLog();
                    courierLog.setErpOrderId(ego.getId());
                    courierLog.setOrderSn(ego.getOrderSn());
                    courierLog.setMailNo(data.get("mail_no") == null ? data.get("mailno").toString() : data.get("mail_no").toString());
                    courierLog.setPartnerId(partnerId);
                    courierLog.setSecret(secret);
                    courierLog.setOrderSerialNo(order.get("order_serial_no").toString());
                    courierLog.setSender(JsonUtil.transferToJson(sender));
                    courierLog.setReceiver(JsonUtil.transferToJson(receiver));
                    courierLog.setItems(JsonUtil.transferToJson(items));
                    courierLog.setType(operationType);
                    courierLog.setCreateBy(ego.getCreatedBy());
                    long currentTime = System.currentTimeMillis() / 1000;
                    courierLog.setCreateAt(currentTime);
                    courierLogService.save(courierLog);
                }

                result.put("code","200");
                result.put("msg","获取快递订单成功");
                result.put("orderSerialNo",order.get("order_serial_no"));
                result.put("erpGoodsOrderList",erpGoodsOrderList);
                result.put("data",resMap.get("data"));
                return result;
            }
            result.put("code","500");
            result.put("msg",resMap.get("data"));
            return result;
        }
        result.put("code","500");
        result.put("msg","类型："+type+"不存在");
        return result;
    }

    /**
     * 获取预览打印的数据
     * @param orderId
     * @param type
     * @param deliveryMode
     * @param orderSn
     * @return
     */
    @GetMapping("/getOrderData")
    public Map getOrderData(String orderId,String deliveryMode,String orderSn){
        // 返回值对象定义
        Map result = new HashMap();
        ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();

        erpGoodsOrder.setOrderStatus(2L);
        if (deliveryMode.equals("1")){
            // 订单全部商品打印
            erpGoodsOrder.setOrderSn(orderSn);
        }else {
            // 订单单独商品打印
            erpGoodsOrder.setId(Long.parseLong(orderId));
        }

        List<ErpGoodsOrder>  erpGoodsOrderList = erpGoodsOrderService.selectOrderList(erpGoodsOrder);

        if (erpGoodsOrderList.isEmpty()){
            result.put("code","500");
            result.put("msg","订单不存在");
            return result;
        }

        // 发货地对象
        Map logisticsMap = null;
        Iterator<ErpGoodsOrder> iterator = erpGoodsOrderList.iterator();
        while (iterator.hasNext()) {
            ErpGoodsOrder ego = iterator.next();
            // 获取订单的商品
            OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(ego.getId());
            if (orderExternalGoods == null) {
                iterator.remove(); // 安全删除
            }else if (null == logisticsMap){
                logisticsMap = zhishuShopGoodsService.selectLogisticsByGoodsId(orderExternalGoods.getGoodsId().toString());
            }
        }

        if (erpGoodsOrderList.isEmpty()){
            result.put("code","500");
            result.put("msg","订单未获取到指定商品信息");
            return result;
        }

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

        erpGoodsOrder = erpGoodsOrderList.get(0);

        // 收货人信息
        JSONObject receiver = new JSONObject();
        receiver.put("name",erpGoodsOrder.getReceiverName());
        // 详细地址 必须添加省市区并以半角逗号隔开
        receiver.put("address",erpGoodsOrder.getProvince()+","+erpGoodsOrder.getCity()+","+erpGoodsOrder.getTown());
        // 手机号
        receiver.put("mobile",erpGoodsOrder.getMobile().replaceAll(".(?=.{4})", "*"));

        // 发件人信息
        JSONObject sender = new JSONObject();
        // 发件人姓名
        sender.put("name",senderName);
        // 详细地址 必须添加省市区并以半角逗号隔开
        sender.put("address",deliveryProvince+","+deliveryCity+","+deliveryArea);
        // 手机号
        sender.put("mobile",phoneNumber.replaceAll(".(?=.{4})", "*"));

        result.put("code","200");
        result.put("msg","获取快递预览数据成功");
        result.put("erpGoodsOrderList",erpGoodsOrderList);
        result.put("receiver",receiver);
        result.put("sender",sender);
        return result;
    }


    @GetMapping("/createBmOrderDaYin")
    public Map createBmOrderDaYin(String mailno,String partnerId,String secret){
        // 返回值对象定义
        Map result = new HashMap();
        // 参数定义
        Map params = new HashMap();
        //也是app-key
        params.put("appid","004064");
        params.put("partner_id",partnerId);
        params.put("secret",secret);
        List<Map> orders = new ArrayList();
        Map order = new HashMap();
        /**
         * 运单号
         */
        order.put("mailno", mailno);
        orders.add(order);
        params.put("orders",orders);
        String jsonData = JsonUtil.transferToJson(params);
        String res = DllInitializer.ydBmGetPdfInfo(jsonData, "004064", "eed7ae222b8541deae79cdfc318b7aa8");
        Map resMap = JsonUtil.transferToObj(res,Map.class);
        if (resMap.get("code").equals("0000") && resMap.get("message").equals("请求成功")){
            List dataList = (List) resMap.get("data");
            Map data = (Map) dataList.get(0);
            result.put("code","200");
            result.put("msg","获取快递订单成功");
            result.put("pdfInfo",data.get("pdfInfo").toString());
            return result;
        }
        result.put("code","500");
        result.put("msg",resMap.get("message").toString());
        return result;
    }

    @GetMapping("/cancelBmOrder")
    public Map cancelBmOrder(String erpOrderId){
        // 返回值对象定义
        Map result = new HashMap();
        List<CourierLog> courierLogList = courierLogService.getListByErpOrderId(Long.parseLong(erpOrderId));

        if (courierLogList.isEmpty()){
            result.put("code","500");
            result.put("msg","未获取到发货记录");
            return result;
        }
        CourierLog courierLog = courierLogList.get(0);

        // 参数定义
        Map params = new HashMap();
        //也是app-key
        params.put("appid","004064");
        params.put("partner_id",courierLog.getPartnerId());
        params.put("secret",courierLog.getSecret());
        List<Map> orders = new ArrayList();
        Map order = new HashMap();
        order.put("order_serial_no", courierLog.getOrderSerialNo());
        order.put("mailno", courierLog.getMailNo());
        orders.add(order);
        params.put("orders",orders);
        String jsonData = JsonUtil.transferToJson(params);
        String res = DllInitializer.ydCancelBmOrder(jsonData, "004064", "eed7ae222b8541deae79cdfc318b7aa8");
        Map resMap = JsonUtil.transferToObj(res,Map.class);
        if (resMap.get("code").equals("0000") && resMap.get("message").equals("请求成功")){
            List dataList = (List) resMap.get("data");
            Map data = (Map) dataList.get(0);
            if (data.get("msg").toString().contains("ERROR")){
                result.put("code","500");
            }else{
                courierLogService.deleteByMailNo(courierLog.getMailNo());

                result.put("code","200");
            }
            result.put("msg",data.get("msg").toString());
            return result;
        }
        result.put("code","500");
        result.put("msg",resMap.get("message").toString());
        return result;
    }

    @GetMapping("/searchCount")
    public Map searchCount(String partnerId,String secret,String type){
        // 返回值对象定义
        Map result = new HashMap();
        // 参数定义
        Map params = new HashMap();
        //也是app-key
        params.put("appid","004064");
        params.put("partner_id",partnerId);
        params.put("secret",secret);
        params.put("type",type);
        String jsonData = JsonUtil.transferToJson(params);
        String res = DllInitializer.ydSearchCount(jsonData, "004064", "eed7ae222b8541deae79cdfc318b7aa8");
        Map resMap = JsonUtil.transferToObj(res,Map.class);
        if (resMap.get("code").equals("0000") && resMap.get("message").equals("请求成功")){
            result.put("code","200");
            result.put("msg","获取电子面单余量成功");
            result.put("data",resMap.get("data"));
            return result;
        }
        result.put("code","500");
        result.put("msg",resMap.get("message").toString());
        return result;
    }
}
