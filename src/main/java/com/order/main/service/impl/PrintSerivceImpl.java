package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.CourierLog;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.OrderExternalGoods;
import com.order.main.service.*;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
public class PrintSerivceImpl implements IPrintSerivce {



    private final IErpGoodsOrderService erpGoodsOrderService;
    private final IOrderExternalGoodsService orderExternalGoodsService;
    private final IZhishuShopGoodsService zhishuShopGoodsService;
    private final ICourierLogService courierLogService;
    private final IZtoPrintService ztoPrintService;


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
    @Override
    public Map createOrder(String orderId, String partnerId, String secret, String type, String cusArea, String deliveryMode, String orderSn,Map logisticsMap){
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
        List<ErpGoodsOrder> erpGoodsOrderList = erpGoodsOrderService.selectOrderList(erpGoodsOrder);
        if (erpGoodsOrderList.isEmpty()){
            result.put("code","500");
            result.put("msg","订单不存在");
            return result;
        }
        // 操作类型
        String operationType = "ADD_ORDER";
        // 发货地对象
        if (logisticsMap == null){
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
        // 发货详细地址
        String fullAddress = logisticsMap.get("full_address").toString();
        // 快递类型
        type = type.equals("yunda") ? "YUNDA" : type;
        if (type.equals("YUNDA")){
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
            // 省
            sender.put("province",deliveryProvince);
            // 市
            sender.put("city",deliveryCity);
            // 区
            sender.put("county",deliveryArea);
            // 详细地址 必须添加省市区并以半角逗号隔开
            sender.put("address",deliveryProvince+","+deliveryCity+","+deliveryArea+","+fullAddress);
            // 手机号
            sender.put("mobile",phoneNumber);
            order.put("sender",sender);
            // 收件人对象
            JSONObject receiver = new JSONObject();
            // 收件人姓名
            receiver.put("name",erpGoodsOrder.getReceiverName());
            // 省
            sender.put("province",erpGoodsOrder.getProvince());
            // 市
            sender.put("city",erpGoodsOrder.getCity());
            // 区
            sender.put("county",erpGoodsOrder.getCountry());
            // 详细地址 必须添加省市区并以半角逗号隔开
            receiver.put("address",erpGoodsOrder.getProvince()+","+erpGoodsOrder.getCity()+","+erpGoodsOrder.getCountry()+","+erpGoodsOrder.getTown());
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
                String mailNo = data.get("mail_no") == null ? data.get("mailno").toString() : data.get("mail_no").toString();
                for (ErpGoodsOrder ego : erpGoodsOrderList){
                    // 日志对象定义
                    CourierLog courierLog = new CourierLog();
                    courierLog.setErpOrderId(ego.getId());
                    courierLog.setOrderSn(ego.getOrderSn());
                    courierLog.setMailNo(mailNo);
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
                    courierLog.setMailType(type);
                    courierLogService.save(courierLog);
                    // 回填快递单号
                    ego.setTrackingNumber(mailNo);
                    erpGoodsOrderService.update(ego);
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
        }else if(type.equals("ZTO")){
            String res = ztoPrintService.createOrder(partnerId,secret,erpGoodsOrderList,logisticsMap);
            Map resMap = JsonUtil.transferToObj(res,Map.class);
            if (resMap.get("message").toString().equals("成功")){
                Map data = (Map) resMap.get("result");
                result.put("code","200");
                result.put("msg","获取快递订单成功");
                result.put("orderSerialNo",data.get("orderCode"));
                result.put("erpGoodsOrderList",erpGoodsOrderList);
                result.put("data",data);
                return result;
            }else{
                result.put("code","500");
                result.put("msg",resMap.get("data"));
                return result;
            }
        }
        result.put("code","500");
        result.put("msg","类型："+type+"不存在");
        return result;
    }

    @Override
    public Map cancelBmOrder(String partnerId, String secret, String orderSn,String mailNo){
        Map result = new HashMap();
        // 参数定义
        Map params = new HashMap();
        //也是app-key
        params.put("appid","004064");
        params.put("partner_id",partnerId);
        params.put("secret",secret);
        List<Map> orders = new ArrayList();
        Map order = new HashMap();
        order.put("order_serial_no", orderSn);
        order.put("mailno", mailNo);
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
                courierLogService.deleteByMailNo(mailNo);
                result.put("code","200");
            }
            result.put("msg",data.get("msg").toString());
        }else{
            result.put("code","500");
            result.put("msg",resMap.get("message").toString());
        }
        return result;
    }
}
