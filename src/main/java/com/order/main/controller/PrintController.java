package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.PostRequest;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.PddSimpleDllLoader;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.*;
import com.order.main.service.*;
import com.order.main.util.DateUtils;
import com.order.main.util.PddUtil;
import com.order.main.util.PrintUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import com.pdd.pop.sdk.http.api.pop.request.PddOpenDecryptBatchRequest;
import lombok.RequiredArgsConstructor;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
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
    private final ISinglePrintService singlePrintService;
    private final IZtoPrintService ztoPrintService;
    private final IPrintSerivce printSerivce;
    private final IExpressDeliveryOrderService expressDeliveryOrderService;
    private final IEmsPrintService emsPrintService;
    private final IJtPrintService jtPrintService;

    /**
     * 创建运单
     * @param orderId       订单id
     * @param type          快递类型
     * @return
     */
    @GetMapping("/createOrder")
    public Map createOrder(String orderId,String partnerId,String secret,String type,String cusArea,String deliveryMode,String orderSn){
        return printSerivce.createOrder(orderId,partnerId,secret,type,cusArea,deliveryMode,orderSn,null);
    }

    @PostMapping("/createOrderNew")
    public Map createOrderNew(@RequestParam Map map){
        return printSerivce.createOrderNew(map);
    }


    /**
     * 批量创建运单号
     * @param partnerId     联调账号
     * @param secret        联调密码
     * @param type          快递类型  YUNDA  ZTO
     * @param orderSns      订单号组
     * @return
     */
    @PostMapping("/createOrderBatch")
    @CrossOrigin(origins = "*")  // 允许所有来源访问
    public Map createOrderBatch(String partnerId,String secret,String type,String orderSn,
                                String contact,String phoneNumber,String province,String city,String area,String town,String remark){
        Map logisticsMap = new HashMap();
        logisticsMap.put("contact",contact);
        logisticsMap.put("phone_number",phoneNumber);
        logisticsMap.put("delivery_province",province);
        logisticsMap.put("delivery_city",city);
        logisticsMap.put("delivery_area",area);
        logisticsMap.put("full_address",town);
        if (type.equals("YUNDA")){
            return  printSerivce.createOrder("",partnerId,secret,type,"","1",orderSn,logisticsMap);
        }else if (type.equals("YZXB") || type.equals("JTSD") || type.equals("YTO")){
            Map map = new HashMap();
            map.put("orderSn",orderSn);
            map.put("deliveryMode","1");
            map.put("logisticsMap",logisticsMap);
            map.put("type",type);
            // 快递账号数据
            Map fastMailMap = new HashMap();
            fastMailMap.put("partnerId",partnerId);
            fastMailMap.put("secret",secret);
            fastMailMap.put("type",type);
            fastMailMap.put("remark",remark);
            map.put("fastMailMap",JsonUtil.transferToJson(fastMailMap));
            return printSerivce.createOrderNew(map);
        }else{
            Map errorMap = new HashMap();
            errorMap.put("code","500");
            errorMap.put("msg","异常类型："+type);
            return errorMap;
        }
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
    @CrossOrigin(origins = "*")  // 允许所有来源访问
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
        ExpressDeliveryOrder expressDeliveryOrder = expressDeliveryOrderService.getByErpOrderId(erpOrderId);
        if (expressDeliveryOrder != null){
            Map fastMail = JsonUtil.transferToObj(expressDeliveryOrder.getFastMailStr(),Map.class);
            if (fastMail.get("type").equals("YZXB")){
                String dataStr = emsPrintService.cancelBmOrder(expressDeliveryOrder.getLogisticsOrderNo(),expressDeliveryOrder.getWaybillNo(),"1",fastMail.get("partnerId").toString(),fastMail.get("secret").toString(),fastMail.get("remark").toString());
                Map dataMap = JsonUtil.transferToObj(dataStr,Map.class);
                if (dataMap.get("retCode").toString().equals("00000")){
                    // 已回收
                    expressDeliveryOrder.setStatus("2");
                    expressDeliveryOrderService.update(expressDeliveryOrder);
                    // 更新订单信息
                    List<ErpGoodsOrder> erpGoodsOrderList = erpGoodsOrderService.selectListByOrderNo(expressDeliveryOrder.getLogisticsOrderNo());
                    for (ErpGoodsOrder erpGoodsOrder : erpGoodsOrderList){
                        erpGoodsOrder.setTrackingNumber("");
                        erpGoodsOrder.setOrderStatus(2L);
                        erpGoodsOrderService.update(erpGoodsOrder);
                    }
                    result.put("code","200");
                    result.put("msg","回收成功");
                }else {
                    result.put("code","500");
                    result.put("msg",dataMap.get("retMsg").toString());
                }
            }else if(fastMail.get("type").equals("JTSD")){
                String dataStr = jtPrintService.jtOrderCancelOrder(fastMail.get("partnerId").toString(),fastMail.get("secret").toString(),expressDeliveryOrder.getLogisticsOrderNo(),"1");
                Map dataMap = JsonUtil.transferToObj(dataStr,Map.class);
                if (dataMap.get("code").equals("1") && dataMap.get("msg").equals("success")){
                    // 已回收
                    expressDeliveryOrder.setStatus("2");
                    expressDeliveryOrderService.update(expressDeliveryOrder);

                    // 更新订单信息
                    List<ErpGoodsOrder> erpGoodsOrderList = erpGoodsOrderService.selectListByOrderNo(expressDeliveryOrder.getLogisticsOrderNo());
                    for (ErpGoodsOrder erpGoodsOrder : erpGoodsOrderList){
                        erpGoodsOrder.setTrackingNumber("");
                        erpGoodsOrder.setOrderStatus(2L);
                        erpGoodsOrderService.update(erpGoodsOrder);
                    }

                    result.put("code","200");
                    result.put("msg","回收成功");
                }else{
                    result.put("code","500");
                    result.put("msg",dataMap.get("retMsg").toString());
                }
                System.out.println(dataMap);
            }else if (fastMail.get("type").equals("YTO")){
                result.put("code","500");
                result.put("msg","圆通不支持回收单号，未发货运单无需回收");
            }
            return result;
        }else{

            List<CourierLog> courierLogList = courierLogService.getListByErpOrderId(Long.parseLong(erpOrderId));

            if (courierLogList.isEmpty()){
                result.put("code","500");
                result.put("msg","未获取到发货记录");
                return result;
            }
            CourierLog courierLog = courierLogList.get(0);
            if (StringUtils.isEmpty(courierLog.getRemark())){
                result = printSerivce.cancelBmOrder(courierLog.getPartnerId(),courierLog.getSecret(),courierLog.getOrderSerialNo(),courierLog.getMailNo());
            }else{
                if (courierLog.getMailType().equals("ZTO")){
                    // 网点中通快递单取消
                    result.put("code","500");
                    result.put("msg","中通全网件快递订单不支持回收");
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
            }

            if (result.get("code").equals("200")){
                ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
                erpGoodsOrder.setTrackingNumber(courierLog.getMailNo());
                List<ErpGoodsOrder> erpGoodsOrderList = erpGoodsOrderService.selectOrderList(erpGoodsOrder);
                for (ErpGoodsOrder ego : erpGoodsOrderList){
                    ego.setTrackingNumber("");
                    erpGoodsOrderService.update(ego);
                }
            }
            return result;
        }
    }

    @PostMapping("/cancelBmOrderApiYunda")
    public Map cancelBmOrderApiYunda(String partnerId,String secret,String orderSn,String mailNo){
        return  printSerivce.cancelBmOrder(partnerId, secret, orderSn, mailNo);
    }

    @PostMapping("/cancelBmOrderApi")
    public Map cancelBmOrderApi(String mailNo) {
        Map result = new HashMap();
        // 邮政、极兔
        List<ExpressDeliveryOrder> expressDeliveryOrderList = expressDeliveryOrderService.getByWaybillNo(mailNo);
        if (!expressDeliveryOrderList.isEmpty()) {
            ExpressDeliveryOrder expressDeliveryOrder = expressDeliveryOrderList.get(0);
            Map fastMail = JsonUtil.transferToObj(expressDeliveryOrder.getFastMailStr(), Map.class);
            if (fastMail.get("type").equals("YZXB")) {
                String dataStr = emsPrintService.cancelBmOrder(expressDeliveryOrder.getLogisticsOrderNo(), expressDeliveryOrder.getWaybillNo(), "1", fastMail.get("partnerId").toString(), fastMail.get("secret").toString(), fastMail.get("remark").toString());
                Map dataMap = JsonUtil.transferToObj(dataStr, Map.class);
                if (dataMap.get("retCode").toString().equals("00000")) {
                    // 已回收
                    expressDeliveryOrder.setStatus("2");
                    expressDeliveryOrderService.update(expressDeliveryOrder);
                    // 更新订单信息
                    List<ErpGoodsOrder> erpGoodsOrderList = erpGoodsOrderService.selectListByOrderNo(expressDeliveryOrder.getLogisticsOrderNo());
                    for (ErpGoodsOrder erpGoodsOrder : erpGoodsOrderList) {
                        erpGoodsOrder.setTrackingNumber("");
                        erpGoodsOrder.setOrderStatus(2L);
                        erpGoodsOrderService.update(erpGoodsOrder);
                    }
                    result.put("code", "200");
                    result.put("msg", "回收成功");
                } else {
                    result.put("code", "500");
                    result.put("msg", dataMap.get("retMsg").toString());
                }
            } else if (fastMail.get("type").equals("JTSD")) {
                String dataStr = jtPrintService.jtOrderCancelOrder(fastMail.get("partnerId").toString(), fastMail.get("secret").toString(), expressDeliveryOrder.getLogisticsOrderNo(), "1");
                Map dataMap = JsonUtil.transferToObj(dataStr, Map.class);
                if (dataMap.get("code").equals("1") && dataMap.get("msg").equals("success")) {
                    // 已回收
                    expressDeliveryOrder.setStatus("2");
                    expressDeliveryOrderService.update(expressDeliveryOrder);

                    // 更新订单信息
                    List<ErpGoodsOrder> erpGoodsOrderList = erpGoodsOrderService.selectListByOrderNo(expressDeliveryOrder.getLogisticsOrderNo());
                    for (ErpGoodsOrder erpGoodsOrder : erpGoodsOrderList) {
                        erpGoodsOrder.setTrackingNumber("");
                        erpGoodsOrder.setOrderStatus(2L);
                        erpGoodsOrderService.update(erpGoodsOrder);
                    }
                    result.put("code", "200");
                    result.put("msg", "回收成功");
                } else {
                    result.put("code", "500");
                    result.put("msg", dataMap.get("retMsg").toString());
                }
                System.out.println(dataMap);
            }else if (fastMail.get("type").equals("YTO")){
                result.put("code","500");
                result.put("msg","圆通不支持回收单号，未发货运单无需回收");
            } else {
                result.put("code", "500");
                result.put("msg", "异常快递类型：" + fastMail.get("type"));
            }
        } else {
            List<CourierLog> courierLogList = courierLogService.getListByMailNo(mailNo);
            if (courierLogList.isEmpty()) {
                result.put("code", "500");
                result.put("msg", "未获取到发货记录");
                return result;
            }

            CourierLog courierLog = courierLogList.get(0);
            if (StringUtils.isEmpty(courierLog.getRemark())) {
                result = printSerivce.cancelBmOrder(courierLog.getPartnerId(), courierLog.getSecret(), courierLog.getOrderSerialNo(), courierLog.getMailNo());
            } else {
                if (courierLog.getMailType().equals("ZTO")) {
                    // 网点中通快递单取消
                    result.put("code", "500");
                    result.put("msg", "中通全网件快递订单不支持回收");
                    return result;
                } else {
                    // 拼多多代打单取消
                    Map fastMailVo = JsonUtil.transferToObj(courierLog.getRemark(), Map.class);
                    // 快递公司编码
                    String wpCode = fastMailVo.get("type").toString();
                    // 发货地址信息
                    Map remarkData = JsonUtil.transferToObj(fastMailVo.get("remark").toString(), Map.class);
                    JSONObject jsonObject = new JSONObject();
                    // 运单号
                    jsonObject.put("waybill_code", courierLog.getMailNo());
                    // 快递公司code
                    jsonObject.put("wp_code", wpCode);
                    String res = PddSimpleDllLoader.executePddApi("PddWaybillCancel", PddUtil.CLIENT_ID, PddUtil.CLIENT_SECRET, remarkData.get("token").toString(), jsonObject.toString());
                    Map resMap = JsonUtil.transferToObj(res, Map.class);
                    if ((Boolean) resMap.get("success")) {
                        courierLogService.deleteByMailNo(courierLog.getMailNo());
                        result.put("code", "200");
                        result.put("msg", "取消成功");
                    } else {
                        result.put("code", "500");
                        result.put("msg", resMap.get("message").toString());
                    }
                }
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

    /**
     * 查询单子面单余额
     * @param expressDeliveryType   快递类型
     * @param account               账号
     * @param password              密码
     * @param json                  接口参数
     * @return
     */
    @GetMapping("/faceSheetBalance")
    public Map faceSheetBalance(String expressDeliveryType,String account,String password,String json){
        Map dataMap = JsonUtil.transferToObj(json,Map.class);
        String res = "";
        if (expressDeliveryType.equals("ZTO")){
            /**
             * recharge         充值数量
             * available        可用数量
             * back             退单数量
             * recovery         回收数量
             */
            res = ztoPrintService.faceSheetBalance(account,password,dataMap);
        }
        Map result = JsonUtil.transferToObj(res, Map.class);
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
            courierLog.setMailType(wpCode);
            courierLogService.save(courierLog);

            ego.setTrackingNumber(waybillCode);
            erpGoodsOrderService.update(ego);
        }
        module.put("erpGoodsOrderList",erpGoodsOrderList);
        return module;
    }


    @GetMapping("/printView")
    public Map printView(String erpOrderId){
        Map result = new HashMap();
        ExpressDeliveryOrder expressDeliveryOrder = expressDeliveryOrderService.getByErpOrderId(erpOrderId);
        if (expressDeliveryOrder != null){
            result.put("code","200");
            result.put("expressDeliveryOrder",expressDeliveryOrder);
            return result;
        }else{
            List<CourierLog> courierLogList = courierLogService.getListByErpOrderId(Long.parseLong(erpOrderId));
            if (courierLogList.isEmpty()){
                result.put("code","500");
                result.put("msg","未获取到发货记录");
                return result;
            }
            CourierLog courierLog = courierLogList.get(0);
            ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
            // 订单全部商品打印
            erpGoodsOrder.setOrderSn(courierLog.getOrderSn());
            List<ErpGoodsOrder>  erpGoodsOrderList = erpGoodsOrderService.selectOrderList(erpGoodsOrder);
            String remark = courierLog.getRemark();

            List itemList = new ArrayList();
            for (ErpGoodsOrder ego : erpGoodsOrderList){
                Map itemMap = JsonUtil.transferToObj(ego.getItemList(),Map.class);
                OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(ego.getId());
                if (orderExternalGoods == null){
                    result.put("code","500");
                    result.put("msg","未获取到下发商品记录");
                    return result;
                }
                ZhishuShopGoods zhishuShopGoods = zhishuShopGoodsService.selectById(Long.parseLong(orderExternalGoods.getGoodsId().toString()));
                if (zhishuShopGoods == null){
                    result.put("code","500");
                    result.put("msg","未获取自营商品信息");
                    return result;
                }
                Map item = new HashMap();
                item.put("goodsName",itemMap.get("goodsName").toString());
                item.put("goodsCount",itemMap.get("goodsCount").toString());
                item.put("isbn",zhishuShopGoods.getIsbn());
                item.put("artNo",zhishuShopGoods.getArtNo());
                item.put("originalArtNo",zhishuShopGoods.getOriginalArtNo());
                itemList.add(item);
            }

            if (courierLog.getMailType().equals("ZTO")){
                Map dataMap = JsonUtil.transferToObj(remark,Map.class);
                Map senderInfo = JsonUtil.transferToObj(courierLog.getSender(),Map.class);
                Map receiveInfo = JsonUtil.transferToObj(courierLog.getReceiver(),Map.class);
                Map data = (Map) dataMap.get("result");
                // 快递单号
                String mailNo = data.get("billCode").toString();
                // 表头
                Map bigMarkInfo = (Map) data.get("bigMarkInfo");
                // title
                String title = bigMarkInfo.get("mark").toString();
                // 集
                String jiStr = bigMarkInfo.get("bagAddr").toString();
                JSONObject sender = new JSONObject();
                sender.put("name",senderInfo.get("senderName").toString());
                sender.put("phone",senderInfo.get("senderMobile").toString());
                sender.put("address",senderInfo.get("senderAddress").toString());
                JSONObject receiver = new JSONObject();
                receiver.put("name",receiveInfo.get("receiverName").toString());
                receiver.put("phone",receiveInfo.get("receiverMobile").toString());
                receiver.put("address",receiveInfo.get("receiverProvince").toString() + receiveInfo.get("receiverCity").toString() + receiveInfo.get("receiverDistrict").toString() + receiveInfo.get("receiverAddress").toString());
                // 构建打印数据
                Map resMap = new HashMap();
                resMap.put("code","200");
                resMap.put("title",title);
                resMap.put("jiStr",jiStr);
                resMap.put("mailNo",mailNo);
                resMap.put("sender",sender);
                resMap.put("receiver",receiver);
                resMap.put("dataList",itemList);
                resMap.put("mailType",courierLog.getMailType());
                resMap.put("fastMailType","1");
                return resMap;
            } else {
                Map fastMailVo = new HashMap();
                if (StringUtils.isEmpty(remark)){
                    fastMailVo.put("fastMailType","1");
                    fastMailVo.put("partnerId",courierLog.getPartnerId());
                    fastMailVo.put("secret",courierLog.getSecret());
                }else{
                    fastMailVo = JsonUtil.transferToObj(remark,Map.class);
                }

                return singlePrintService.printView(fastMailVo,courierLog.getMailNo(),courierLog.getOrderSn(),itemList);
            }
        }
    }


    /**
     * 根据运单号查询订单信息
     * @return
     */
    @GetMapping("/printViewByWaybillNo")
    @CrossOrigin(origins = "*")  // 允许所有来源访问
    public Map printViewByWaybillNo(String waybillNo){
        System.out.println("运单号："+waybillNo);
        Map result = new HashMap();
        List<ExpressDeliveryOrder> expressDeliveryOrderList = expressDeliveryOrderService.getByWaybillNo(waybillNo);
        if (!expressDeliveryOrderList.isEmpty()){
            result.put("code","200");
            result.put("expressDeliveryOrder",expressDeliveryOrderList.get(0));
            return result;
        }else{
            List<CourierLog> courierLogList = courierLogService.getListByMailNo(waybillNo);
            if (courierLogList.isEmpty()){
                result.put("code","500");
                result.put("msg","未获取到发货记录");
                return result;
            }
            CourierLog courierLog = courierLogList.get(0);
            ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
            // 订单全部商品打印
            erpGoodsOrder.setOrderSn(courierLog.getOrderSn());
            List<ErpGoodsOrder>  erpGoodsOrderList = erpGoodsOrderService.selectOrderList(erpGoodsOrder);
            String remark = courierLog.getRemark();

            List itemList = new ArrayList();
            for (ErpGoodsOrder ego : erpGoodsOrderList){
                Map itemMap = JsonUtil.transferToObj(ego.getItemList(),Map.class);
                OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(ego.getId());
                Map item = new HashMap();
                if (orderExternalGoods == null){
                    List items = JsonUtil.transferToObj(courierLog.getItems(),List.class);

                    Map itemData = (Map) items.get(0);
                    item.put("itemName",itemData.get("name").toString());
                    item.put("itemNum",itemData.get("number").toString());
                }else{
                    ZhishuShopGoods zhishuShopGoods = zhishuShopGoodsService.selectById(Long.parseLong(orderExternalGoods.getGoodsId().toString()));
                    if (zhishuShopGoods == null){
                        result.put("code","500");
                        result.put("msg","未获取自营商品信息");
                        return result;
                    }
                    item.put("goodsName",itemMap.get("goodsName").toString());
                    item.put("goodsCount",itemMap.get("goodsCount").toString());
                    item.put("isbn",zhishuShopGoods.getIsbn());
                    item.put("artNo",zhishuShopGoods.getArtNo());
                    item.put("originalArtNo",zhishuShopGoods.getOriginalArtNo());
                }
                itemList.add(item);

            }

            if (courierLog.getMailType().equals("ZTO")){
                Map dataMap = JsonUtil.transferToObj(remark,Map.class);
                Map senderInfo = JsonUtil.transferToObj(courierLog.getSender(),Map.class);
                Map receiveInfo = JsonUtil.transferToObj(courierLog.getReceiver(),Map.class);
                Map data = (Map) dataMap.get("result");
                // 快递单号
                String mailNo = data.get("billCode").toString();
                // 表头
                Map bigMarkInfo = (Map) data.get("bigMarkInfo");
                // title
                String title = bigMarkInfo.get("mark").toString();
                // 集
                String jiStr = bigMarkInfo.get("bagAddr").toString();
                JSONObject sender = new JSONObject();
                sender.put("name",senderInfo.get("senderName").toString());
                sender.put("phone",senderInfo.get("senderMobile").toString());
                sender.put("address",senderInfo.get("senderAddress").toString());
                JSONObject receiver = new JSONObject();
                receiver.put("name",receiveInfo.get("receiverName").toString());
                receiver.put("phone",receiveInfo.get("receiverMobile").toString());
                receiver.put("address",receiveInfo.get("receiverProvince").toString() + receiveInfo.get("receiverCity").toString() + receiveInfo.get("receiverDistrict").toString() + receiveInfo.get("receiverAddress").toString());
                // 构建打印数据
                Map resMap = new HashMap();
                resMap.put("code","200");
                resMap.put("title",title);
                resMap.put("jiStr",jiStr);
                resMap.put("mailNo",mailNo);
                resMap.put("sender",sender);
                resMap.put("receiver",receiver);
                resMap.put("dataList",itemList);
                resMap.put("mailType",courierLog.getMailType());
                resMap.put("fastMailType","1");
                return resMap;
            } else {
                Map fastMailVo = new HashMap();
                if (StringUtils.isEmpty(remark)){
                    fastMailVo.put("fastMailType","1");
                    fastMailVo.put("partnerId",courierLog.getPartnerId());
                    fastMailVo.put("secret",courierLog.getSecret());
                }else{
                    fastMailVo = JsonUtil.transferToObj(remark,Map.class);
                }

                return singlePrintService.printView(fastMailVo,courierLog.getMailNo(),courierLog.getOrderSn(),itemList);
            }
        }
    }
}
