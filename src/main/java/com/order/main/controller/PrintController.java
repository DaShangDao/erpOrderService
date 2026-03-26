package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.PostRequest;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.PddSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.CourierLog;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.OrderExternalGoods;
import com.order.main.entity.Shop;
import com.order.main.service.ICourierLogService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IOrderExternalGoodsService;
import com.order.main.service.IZhishuShopGoodsService;
import com.order.main.util.PddUtil;
import com.order.main.util.PrintUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import com.pdd.pop.sdk.http.api.pop.request.PddOpenDecryptBatchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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


    /**
     * 获取快递订单pdf文件
     * @param mailno
     * @param partnerId
     * @param secret
     * @return
     */
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

    /**
     * 电子面单取消
     * @param erpOrderId
     * @return
     */
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
        if (StringUtils.isEmpty(courierLog.getRemark())){
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
        }else{
            // 拼多多代打单取消
            Map fastMailVo = JsonUtil.transferToObj(courierLog.getRemark(),Map.class);
            // 快递公司编码
            String wpCode = fastMailVo.get("type").toString();
            // 发货地址信息
            Map remarkData = JsonUtil.transferToObj(fastMailVo.get("remark").toString(),Map.class);
            JSONObject jsonObject = new JSONObject();
            // 运单号
            jsonObject.put("waybill_code",courierLog.getMailNo());
            // 快递公司code
            jsonObject.put("wp_code",wpCode);
            String res = PddSimpleDllLoader.executePddApi("PddWaybillCancel", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,remarkData.get("token").toString(), jsonObject.toString());
            Map resMap = JsonUtil.transferToObj(res,Map.class);
            if ((Boolean) resMap.get("success")){
                courierLogService.deleteByMailNo(courierLog.getMailNo());
                result.put("code","200");
                result.put("msg","取消成功");
            }else {
                result.put("code","500");
                result.put("msg",resMap.get("message").toString());
            }
        }
        return result;
    }

    /**
     * 韵达查询单子面单余额
     * @param partnerId
     * @param secret
     * @param type
     * @return
     */
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



    @GetMapping("/getFastMail")
    public String getFastMail(@RequestParam Map map){
        String shopType = map.get("shopType").toString();
        if (shopType.equals("1")){
            JSONObject jsonObject = new JSONObject();
            String res = PddSimpleDllLoader.executePddApi("PddWaybillSearch", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,map.get("token").toString(), jsonObject.toString());
            System.out.println( res);
            return res;
        }
        return null;
    }


    @PostMapping("/pddCreateOrder")
    public Map pddCreateOrder(@RequestParam Map map){
        // 返回值对象定义
        Map result = new HashMap();
        // 打印模式  1 全部打印  2 单独打印
        String deliveryMode = map.get("deliveryMode").toString();
        String orderSn = map.get("orderSn").toString();
        String erpOrderId = map.get("erpOrderId").toString();
        // 面单账号信息
        Map fastMailVo = JsonUtil.transferToObj(map.get("fastMailVo").toString(),Map.class);
        // 快递公司编码
        String wpCode = fastMailVo.get("type").toString();
        // 发货地址信息
        Map remarkData = JsonUtil.transferToObj(fastMailVo.get("remark").toString(),Map.class);


        ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
        erpGoodsOrder.setOrderStatus(2L);
        if (deliveryMode.equals("1")){
            // 订单全部商品打印
            erpGoodsOrder.setOrderSn(orderSn);
        }else {
            // 订单单独商品打印
            erpGoodsOrder.setId(Long.parseLong(erpOrderId));
        }

        List<ErpGoodsOrder>  erpGoodsOrderList = erpGoodsOrderService.selectOrderList(erpGoodsOrder);

        if (erpGoodsOrderList.isEmpty()){
            result.put("code","500");
            result.put("msg","订单不存在");
            return result;
        }

        // 获取ERP仓库发货信息
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
        // 操作类型
        String operationType = "ADD_ORDER";

        erpGoodsOrder = erpGoodsOrderList.get(0);

        // 联系人/发货人
        String senderName = logisticsMap.get("contact").toString();
        // 联系电话
        String phoneNumber = logisticsMap.get("phone_number").toString();

        // 入参信息
        JSONObject paramWaybillCloudPrintApplyNewRequest = new JSONObject();
        /**
         * 发货人信息
         */
        JSONObject sender = new JSONObject();
        // 发货地址，需要入参与 search 接口中的发货人地址信息一致
        JSONObject address = new JSONObject();
        address.put("country",remarkData.get("country").toString());         // 国家
        address.put("province",remarkData.get("province").toString());       // 省
        address.put("city",remarkData.get("city").toString());               // 市
        address.put("district",remarkData.get("district").toString());       // 区
        address.put("detail",remarkData.get("detail").toString());           // 详细地址
        sender.put("address",address);
        // 手机号码
        sender.put("mobile",phoneNumber);
        // 发货人
        sender.put("name",senderName);
        // 固定电话
        sender.put("phone",phoneNumber);
        /**
         * 取号列表
         */
        List<JSONObject> tradeOrderInfoDtos = new ArrayList<>();
        // 取号对象
        JSONObject tradeOrderInfoDto = new JSONObject();
        // 请求id
        tradeOrderInfoDto.put("object_id",orderSn);
        // 订单信息
        JSONObject orderInfo = new JSONObject();
        // 订单渠道平台编码
        orderInfo.put("order_channels_type","OTHERS");
        // 订单号 数量限制100
        List<String> tradeOrderList = new ArrayList<>();
        // 订单号
        tradeOrderList.add(orderSn);
        orderInfo.put("trade_order_list",tradeOrderList);
        // 包裹信息
        JSONObject packageInfo = new JSONObject();
        List<JSONObject> items = new ArrayList<>();
        for (ErpGoodsOrder ego : erpGoodsOrderList){
            // 商品信息
            GoodsDto goodsDto = JsonUtil.transferToObj(ego.getItemList(),GoodsDto.class);
            // 单个商品列表
            JSONObject item = new JSONObject();
            // 商品名称
            item.put("name",goodsDto.getGoodsName());
            // 商品数量
            item.put("count",goodsDto.getGoodsCount());
            items.add(item);
        }
        packageInfo.put("items",items);
        tradeOrderInfoDto.put("order_info",orderInfo);
        tradeOrderInfoDto.put("package_info",packageInfo);
        // 收件人信息
        JSONObject recipient = new JSONObject();
        // 收件人地址
        JSONObject recipientAddress = new JSONObject();
        // 省
        recipientAddress.put("province",erpGoodsOrder.getProvince());
        // 市
        recipientAddress.put("city",erpGoodsOrder.getCity());
        // 区
        recipientAddress.put("district",erpGoodsOrder.getCountry());
        // 详细地址
        recipientAddress.put("detail",erpGoodsOrder.getTown());
        recipient.put("address",recipientAddress);
        // 手机号
        recipient.put("mobile",erpGoodsOrder.getMobile());
        // 收件人姓名
        recipient.put("name",erpGoodsOrder.getReceiverName());

        tradeOrderInfoDto.put("recipient",recipient);
        // 标准模板模板URL
        tradeOrderInfoDto.put("template_url",PddUtil.getCloudprintStdtemplates(wpCode));
        // 使用者ID
        tradeOrderInfoDto.put("user_id",remarkData.get("mallId").toString());

        tradeOrderInfoDtos.add(tradeOrderInfoDto);
        paramWaybillCloudPrintApplyNewRequest.put("sender",sender);
        paramWaybillCloudPrintApplyNewRequest.put("trade_order_info_dtos",tradeOrderInfoDtos);
        paramWaybillCloudPrintApplyNewRequest.put("wp_code",wpCode);

        String json = paramWaybillCloudPrintApplyNewRequest.toString();

        String res = PddSimpleDllLoader.executePddApi("PddWaybillGet", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,remarkData.get("token").toString(), json);

        Map resMap = JsonUtil.transferToObj(res,Map.class);

        Map pddWaybillGetResponse = (Map)resMap.get("pdd_waybill_get_response");

        List modules = (List)pddWaybillGetResponse.get("modules");

        Map module = (Map)modules.get(0);
        // 运单号
        String waybillCode = module.get("waybill_code").toString();

        for (ErpGoodsOrder ego : erpGoodsOrderList){
            // 日志对象定义
            CourierLog courierLog = new CourierLog();
            courierLog.setErpOrderId(ego.getId());
            courierLog.setOrderSn(ego.getOrderSn());
            courierLog.setMailNo(waybillCode);
            courierLog.setPartnerId(fastMailVo.get("partnerId").toString());
            courierLog.setSecret("");
            courierLog.setOrderSerialNo(orderSn);
            courierLog.setSender(sender.toString());
            courierLog.setReceiver(recipient.toString());
            courierLog.setItems(JsonUtil.transferToJson(items));
            courierLog.setType(operationType);
            courierLog.setCreateBy(ego.getCreatedBy());
            long currentTime = System.currentTimeMillis() / 1000;
            courierLog.setCreateAt(currentTime);
            courierLog.setRemark(map.get("fastMailVo").toString());
            courierLogService.save(courierLog);
        }
        module.put("erpGoodsOrderList",erpGoodsOrderList);
        return module;
    }
}
