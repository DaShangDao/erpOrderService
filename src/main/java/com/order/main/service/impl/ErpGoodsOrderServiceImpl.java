package com.order.main.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dto.GoodsCheckRejectVo;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.*;
import com.order.main.service.*;
import com.order.main.service.client.ShopGoodsPublishedClient;
import com.order.main.util.*;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.http.api.pop.request.PddOpenDecryptBatchRequest;
import com.pdd.pop.sdk.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.order.main.mapper.ErpGoodsOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 平台订单Service业务层处理
 *
 * @author yxy
 * @date 2025-12-04
 */
@RequiredArgsConstructor
@Service
public class ErpGoodsOrderServiceImpl implements IErpGoodsOrderService {

    //拼多多手续费
    private int pddPercentageInt = 0;
    //孔夫子订单手续费
    private int kfzPercentageInt = 0;
    //咸鱼订单手续费
    private int xyPercentageInt = 0;

    private final ErpGoodsOrderMapper baseMapper;
    private final IShopGoodsPublishedService shopGoodsPublishedService;
    private final IZhishuShopGoodsService zhishuShopGoodsService;
    private final IEditStockService editStockService;
    private final ISysUserService userService;
    private final LogisticsService logisticsService;
    private final IOrderExternalGoodsService orderExternalGoodsService;
    private final IProfitconfigService profitconfigService;
    private final IWarehouseSettingsService warehouseSettingsService;
    private final IErpGoodsOrderAccountsService erpGoodsOrderAccountsService;
    private final IUserSettingsAttributeService userSettingsAttributeService;
    private final IShopService shopService;
    private final ShopGoodsPublishedClient shopGoodsPublishedClient;
    private final RedisService redisService;
    private final IRunningTaskService runningTaskService;

    @Autowired
    private TokenUtils tokenUtils;


    /**
     * 获取不同平台的手续费率（整数百分比）
     *
     * 根据店铺类型shopType获取对应的平台手续费率
     * 支持的手续费类型：
     * - 1: 拼多多平台 (pddOrder)
     * - 2: 孔夫子平台 (kfzOrder)
     * - 5: 闲鱼平台 (goofishOrder)
     * 默认返回10%（当shopType不匹配时）
     *
     * @param shopType 店铺类型字符串，对应不同的电商平台
     * @return int 手续费率百分比，整数形式（如10表示10%）
     */
    public int getPercentageInt(String shopType) {
        // 拼多多平台手续费计算
        if (shopType.equals("1")) {
            // 使用懒加载模式，只在第一次访问时从服务获取配置值
            if (pddPercentageInt == 0) {
                // 从profitconfigService获取拼多多平台手续费配置
                pddPercentageInt = profitconfigService.getProfitconfigList("pddOrder");
            }
            return pddPercentageInt;
        }

        // 孔夫子平台手续费计算
        if (shopType.equals("2")) {
            // 使用懒加载模式，只在第一次访问时从服务获取配置值
            if (kfzPercentageInt == 0) {
                // 从profitconfigService获取孔夫子平台手续费配置
                kfzPercentageInt = profitconfigService.getProfitconfigList("kfzOrder");
            }
            return kfzPercentageInt;
        }

        // 闲鱼平台手续费计算
        if (shopType.equals("5")) {
            // 使用懒加载模式，只在第一次访问时从服务获取配置值
            if (xyPercentageInt == 0) {
                // 从profitconfigService获取闲鱼平台手续费配置
                xyPercentageInt = profitconfigService.getProfitconfigList("goofishOrder");
            }
            return xyPercentageInt;
        }

        // 默认手续费率：10%（当店铺类型未匹配到任何已知平台时）
        return 10;
    }

    /**
     * 根据id查询订单数据
     * @param id    erp订单id
     * @return
     */
    @Override
    public ErpGoodsOrder selectById(Long id){
        return baseMapper.selectById(id);
    }

    @Override
    public int xyOrderPush(Shop shop, String orderNo,Boolean manua){
        // 获取订单明细
        Map getOrderDetailMap = new HashMap();
        // 咸鱼的appId
        getOrderDetailMap.put("appId", shop.getMallId());
        // 咸鱼的appSecret
        getOrderDetailMap.put("appSecret",shop.getToken());
        // 订单号
        getOrderDetailMap.put("orderNo",orderNo);
        // 查询订单明细数据
        String orderDetailStr = InterfaceUtils.postForm("http://119.45.237.193:9102","/xianyv/getOrderDetail",getOrderDetailMap);
        // 将返回值转为Map
        Map orderDetailMap = JsonUtil.transferToObj(orderDetailStr, Map.class);
        // 从Map中获取data参数，返回值数据
        Map orderDetailDataMap = (Map) orderDetailMap.get("data");
        // 从data中解析商品数据
        Map goodsMap = (Map) orderDetailDataMap.get("goods");
        // 解析商品信息
        GoodsDto goodsDto = new GoodsDto();
        // 商品编号
        goodsDto.setGoodsId(goodsMap.get("item_id").toString());
        // 商品名称
        goodsDto.setGoodsName(goodsMap.get("title").toString());
        // 商品数量
        goodsDto.setGoodsCount(goodsMap.get("quantity").toString());
        // 商品销售价格
        goodsDto.setGoodsPrice(goodsMap.get("price").toString());
        // 商品规格
        goodsDto.setGoodsSpec(goodsMap.get("sku_text").toString());
        // 商品图片
        goodsDto.setGoodsImgs((List<String>) goodsMap.get("images"));
        // 商家外部编码
        goodsDto.setOuterGoodsId(goodsMap.get("outer_id").toString());
        // 闲管家商品id
        goodsDto.setOuterId(goodsMap.get("product_id").toString());
        // 规格编码
        goodsDto.setSkuId(goodsMap.get("sku_id").toString());
        // 根据订单号查询erp订单是否存在
        ErpGoodsOrder erpGoodsOrder = selectByOrderNo(orderNo);
        // erpGoodsOrder 非null判断
        if(erpGoodsOrder == null){
            // 获取新增订单对象
            erpGoodsOrder = getAddErpGoodsOrder(orderNo,shop,goodsDto,orderDetailDataMap);
        }else{
            // 获取修改订单对象
            erpGoodsOrder = getEditErpGoodsOrder(erpGoodsOrder,goodsDto,orderDetailDataMap);
        }
        // 订单具体操作方法
        orderOperation(shop,erpGoodsOrder,manua);
        return 1;
    }

    /**
     * 孔夫子订单方法
     * manua 是否是手动订单同步  true 是  false  否
     */
    @Override
    public void kfzOrderPush(Shop shop, List orderList,Boolean manua){
        // 循环订单列表
        for (int i = 0; i < orderList.size(); i++){
            // 获取订单对象
            Map map = (Map) orderList.get(i);
            // 订单编号
            String orderSn = map.get("orderId").toString();
            if(map.get("orderStatus").equals("ConfirmedToPay")){
                try {
                    // 如果是待付款的订单则需要等待2s中重新查一次订单信息
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.out.println("休息异常");
                }
                // 如果是待付款订单则需要等待2s重新查询一次订单信息，防止将支付订单给忽略了
                String result = DllInitializer.executeKongfzOrderGet(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,shop.getToken(),"seller",Integer.parseInt(orderSn));
                Map resultMap = JsonUtil.transferToObj(result, Map.class);
                // 解析成功信息
                map = (Map) resultMap.get("successResponse");
            }
            // 解析订单状态和售后状态
            List<Long> orderStatusList = OrderUtils.kfzGetOrderStatus(map.get("orderStatus").toString());
            // 获取订单状态
            Long orderStatus = orderStatusList.get(0);
            // 售后状态
            Long afterSalesStatus = orderStatusList.get(1);
            // 获取收货地
            Map receiverInfo = (Map) map.get("receiverInfo");
            // 订单商品信息
            List<Map> orderGoodsList = (List<Map>) map.get("items");
            // 循环订单中的商品列表
            for (int j = 0; j < orderGoodsList.size(); j++){
                // 获取商品对象
                Map item = orderGoodsList.get(j);
                // 解析商品信息
                GoodsDto goodsDto = new GoodsDto();
                // 商品编号
                goodsDto.setGoodsId(item.get("itemId").toString());
                // 商品名称
                goodsDto.setGoodsName(item.get("itemName").toString());
                // 商品数量
                goodsDto.setGoodsCount(item.get("number").toString());
                // 商品销售价格
                goodsDto.setGoodsPrice(new BigDecimal(item.get("price").toString()).multiply(BigDecimal.valueOf(100)).toString());
                // 商品图片
                List<String> goodsImgs = new ArrayList<>();
                goodsImgs.add( item.get("img").toString());
                goodsDto.setGoodsImgs(goodsImgs);
                // 商家外部编码
                goodsDto.setOuterGoodsId(item.get("itemSn").toString());
                // 定义订单对象
                ErpGoodsOrder erpGoodsOrder = null;
                try{
                    // id查询erp订单是否存在
                    erpGoodsOrder = selectBoOrderNoAndGoodsId(orderSn,goodsDto.getGoodsId());
                } catch (Exception e) {
                    System.out.println("查询异常,异常参数:订单号:"+orderSn+";商品id:"+goodsDto.getGoodsId());
                    // 打印异常
                    e.printStackTrace();
                }
                // erpGoodsOrder 非null判断
                if(erpGoodsOrder == null){
                    // 新建订单对象
                    erpGoodsOrder = new ErpGoodsOrder();
                    // 订单编号
                    erpGoodsOrder.setOrderSn(orderSn);
                    // 平台店铺id
                    erpGoodsOrder.setShopId(shop.getMallId()+"");
                    // 店铺类型
                    erpGoodsOrder.setShopType(Long.parseLong(shop.getShopType()));
                    // erp店铺id
                    erpGoodsOrder.setShopErpId(shop.getId());
                    // erp店铺名称
                    erpGoodsOrder.setShopErpName(shop.getShopName());
                    // 订单中商品sku列表json字符串
                    erpGoodsOrder.setItemList(JsonUtil.transferToJson(goodsDto));
                    //订单价格（支付价格）
                    Long payAmount = new BigDecimal(getStringValue(map, "orderAmount")).multiply(BigDecimal.valueOf(100)).longValue();
                    erpGoodsOrder.setOrderTotal(payAmount);
                    // 支付金额 (以分为单位)
                    erpGoodsOrder.setPayAmount(payAmount);
                    // 折扣金额
                    erpGoodsOrder.setOrderChangeAmount(new BigDecimal(getStringValue(map, "favorableMoney")).multiply(BigDecimal.valueOf(100)).longValue());
                    // 商品金额（以分为单位）商品金额=商品销售价格*商品数量-订单改价折扣金额
                    erpGoodsOrder.setGoodsAmount(new BigDecimal(getStringValue(map, "goodsAmount")).multiply(BigDecimal.valueOf(100)).longValue());
                    // 创建人
                    erpGoodsOrder.setCreatedBy(Long.parseLong(shop.getCreateBy()));
                    // 创建时间 时间戳
                    erpGoodsOrder.setCreatedAt(TimestampConverter.toTimestamp(map.get("createdTime").toString()));
                    // 是否可视化
                    erpGoodsOrder.setIsShow(0L);
                    // 是否下发订单 0 否 1 是
                    erpGoodsOrder.setIsIssue(0L);
                }
                // 订单中商品sku列表对象
                erpGoodsOrder.setGoodsDto(goodsDto);
                // 修改用，修改前的订单状态
                erpGoodsOrder.setOldOrderStatus(erpGoodsOrder.getOrderStatus());
                // 订单类型转换
                erpGoodsOrder.setOrderStatus(orderStatus);
                if (orderStatus == 4L){
                    // 成交状态 0 未成交 1 成交
                    erpGoodsOrder.setConfirmStatus(1L);
                    // 成交时间 时间戳
                    erpGoodsOrder.setConfirmAt(TimestampConverter.toTimestamp(map.get("createdTime").toString()));
                }else{
                    // 成交状态 0 未成交 1 成交
                    erpGoodsOrder.setConfirmStatus(0L);
                }
                // 如果省为空再进行获取地址信息
                if (StringUtils.isEmpty(erpGoodsOrder.getProvince()) || StringUtils.isEmpty(erpGoodsOrder.getTown())) {
                    erpGoodsOrder.setProvince(receiverInfo.get("provName") == null ? "" : receiverInfo.get("provName").toString());
                    erpGoodsOrder.setCity(receiverInfo.get("cityName") == null ? "" : receiverInfo.get("cityName").toString());
                    erpGoodsOrder.setCountry(receiverInfo.get("areaName") == null ? "" : receiverInfo.get("areaName").toString());
                    erpGoodsOrder.setTown(receiverInfo.get("address") == null || receiverInfo.get("address").toString().contains("*****") ? "" : receiverInfo.get("address").toString());
                    erpGoodsOrder.setReceiverName(receiverInfo.get("receiverName") == null ? "" : receiverInfo.get("receiverName").toString());
                    erpGoodsOrder.setMobile(receiverInfo.get("mobile") == null ? "" : receiverInfo.get("mobile").toString());
                }
                // 快递单号
                erpGoodsOrder.setTrackingNumber(getStringValue(map, "shipmentNum"));
                // 修改前售后状态
                erpGoodsOrder.setOldAfterSalesStatus(erpGoodsOrder.getAfterSalesStatus());
                // 售后状态
                erpGoodsOrder.setAfterSalesStatus(afterSalesStatus);
                // 订单最近一次更新时间
                erpGoodsOrder.setUpdatedAt(TimestampConverter.toTimestamp(map.get("createdTime").toString()));
                // 订单具体操作方法
                orderOperation(shop,erpGoodsOrder,manua);
            }
        }
    }

    @Override
    public void pddManualOrder(Shop shop,String taskId,String startUpdateTime,String endUpdateTime,Boolean manua){
        long startTime = DateUtils.parseDateTimeToTimestamp(startUpdateTime) / 1000;
        long endTime = DateUtils.parseDateTimeToTimestamp(endUpdateTime) / 1000;

        int page = 1;
        int pageSize = 100;

        // 按天循环
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime * 1000);

        while (true) {
            // 日志记录
            List<RunningTask> runningTaskList = new ArrayList<>();

            // 获取当前天的开始时间（0点0分0秒）
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long dayStartTime = calendar.getTimeInMillis() / 1000;

            // 获取当前天的结束时间（23点59分59秒）
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            long dayEndTime = calendar.getTimeInMillis() / 1000;

            // 确保不会超过总的结束时间
            if (dayStartTime > endTime) {
                break;
            }

            if (dayEndTime > endTime) {
                dayEndTime = endTime;
            }

            String result = InterfaceUtils.getInterface("http://pdd.buzhiyushu.cn",
                    "/api/pdd/auth/getOrderList?accessToken=" + shop.getToken() +
                            "&startTime=" + dayStartTime +
                            "&endTime=" + dayEndTime +
                            "&page=" + page +
                            "&pageSize=" + pageSize);

            System.out.println("查询日期：" + new Date(dayStartTime * 1000) + " 到 " + new Date(dayEndTime * 1000));
            Map resultMap = JsonUtil.transferToObj(result, Map.class);
            Boolean hasNext = (Boolean) resultMap.get("has_next");
            List orderList = (List) resultMap.get("order_list");

            for (int i = 0; i < orderList.size(); i++){
                Map orderMap = (Map) orderList.get(i);
                String orderSn = orderMap.get("order_sn").toString();

                // 定义日志数据
                RunningTask runningTask = new RunningTask();
                runningTask.setTaskId(Long.parseLong(taskId));
                runningTask.setShopId(shop.getId());
                runningTask.setGoodsId(0L);
                runningTask.setRandomNum(System.currentTimeMillis());
                runningTask.setTaskName("手动订单库存同步");
                runningTask.setPriority(255L);
                runningTask.setData("");
                runningTask.setStatus("3");
                runningTask.setTaskType("MANUAL_ORDER");
                String callBackData = "";

                Long orderStatus = OrderUtils.pddGetOrderStatus(Integer.parseInt(orderMap.get("order_status").toString()));
                Long afterSalesStatus = Long.parseLong(orderMap.get("after_sales_status").toString());

                if (orderStatus == 1 ){
                    callBackData = "订单号："+orderSn+";订单状态：待付款;跳过";
                }else if (orderStatus == 2 && afterSalesStatus == 10){
                    callBackData = "订单号："+orderSn+";订单状态：待发货;售后状态：退款完成;跳过";
                }else{
                    List<Map> itemList = (List<Map>) orderMap.get("item_list");

                    for (int j = 0; j < itemList.size(); j++){
                        Map item = (Map) itemList.get(j);
                        // 商品id
                        String itemId = item.get("goods_id").toString();

                        // 定义订单对象
                        ErpGoodsOrder erpGoodsOrder = null;
                        try{
                            // id查询erp订单是否存在
                            erpGoodsOrder = selectBoOrderNoAndGoodsId(orderSn,itemId);
                        } catch (Exception e) {
                            callBackData = "查询异常,异常参数:订单号:"+orderSn+";商品id:"+itemId;
                            // 打印异常
                            e.printStackTrace();
                        }
                        if (erpGoodsOrder == null){
                            Message message = new Message();
                            message.setMallID(shop.getMallId());
                            Map contentMap = new HashMap();
                            contentMap.put("tid",orderSn);
                            message.setContent(JsonUtil.transferToJson(contentMap));
                            pddOrderPush(message,manua);
                            callBackData += "订单号："+orderSn+";商品id："+itemId+";重新执行订单库存同步操作;";
                        }else{
                            // 校验是否下发
                            OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(erpGoodsOrder.getId());
                            // 未下发则重新执行下单操作
                            if (orderExternalGoods == null){
                                Message message = new Message();
                                message.setMallID(shop.getMallId());
                                Map contentMap = new HashMap();
                                contentMap.put("tid",orderSn);
                                message.setContent(JsonUtil.transferToJson(contentMap));
                                pddOrderPush(message,manua);
                                callBackData += "订单号："+orderSn+";商品id："+itemId+";订单未下发，重新执行下发流程;";
                            }else{
                                callBackData += "订单号："+orderSn+";商品id："+itemId+";订单已下发;";
                            }
                        }
                    }
                }
                runningTask.setCallBackData(callBackData);
                runningTaskList.add(runningTask);
            }

            if (runningTaskList.size() > 0){
                runningTaskService.batchInsert(runningTaskList);
            }

            if (hasNext){
                page++;
            }else{
                // 增加一天
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                page = 1;
            }
        }
    }
    @Override
    public void kfzManualOrder(Shop shop,String taskId,String startUpdateTime,String endUpdateTime,Boolean manua){
        // 定义分页
        int pageNum = 1;
        boolean isRefreshToken = false;
        JSONObject jsonObject = new JSONObject();
        // 查询方
        jsonObject.put("userType","seller");
        jsonObject.put("startUpdateTime",startUpdateTime);
        jsonObject.put("endUpdateTime",endUpdateTime);
        int runMark = 0;

        List newOrderList = new ArrayList();

        while(true){
            // 日志记录
            List<RunningTask> runningTaskList = new ArrayList<>();

            // 分页信息
            jsonObject.put("pageNum",pageNum);
            // 每页50条
            jsonObject.put("pageSize",50);
            String json = jsonObject.toString();
            // 调用dll 查询订单列表
            String result = DllInitializer.executeKongfzOrderList(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,shop.getToken(),json);
            // 转为Map对象
            Map resultMap = null;
            try{
                resultMap = JsonUtil.transferToObj(result, Map.class);
            }catch (Exception e) {
                System.out.println("解析异常数据-----------：" + result);
                runMark++;
                if (runMark > 3) {
                    break;
                } else {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        System.out.println("休息异常--------");
                    }
                    continue;
                }
            }
            // 错误信息为空的情况
            if(null == resultMap.get("errorResponse")){
                // 解析成功信息
                Map successResponse = (Map) resultMap.get("successResponse");
                // 获取订单列表
                List orderList = (List) successResponse.get("list");
                // 手动调用订单方法
                for (int i = 0; i < orderList.size(); i++){

                    // 定义日志数据
                    RunningTask runningTask = new RunningTask();
                    runningTask.setTaskId(Long.parseLong(taskId));
                    runningTask.setShopId(shop.getId());
                    runningTask.setGoodsId(0L);
                    runningTask.setRandomNum(System.currentTimeMillis());
                    runningTask.setTaskName("手动订单库存同步");
                    runningTask.setPriority(255L);
                    runningTask.setData("");
                    runningTask.setStatus("3");
                    runningTask.setTaskType("MANUAL_ORDER");

                    String callBackData = "";

                    Map order = (Map) orderList.get(i);
                    // 订单号
                    String orderSn = order.get("orderId").toString();
                    // 解析订单状态和售后状态
                    List<Long> orderStatusList = OrderUtils.kfzGetOrderStatus(order.get("orderStatus").toString());
                    // 获取订单状态
                    Long orderStatus = orderStatusList.get(0);

                    if (orderStatus == 1){
                        callBackData = "订单号："+orderSn+";订单状态：待付款;跳过";
                    }else {
                        List items = (List) order.get("items");

                        for (int j = 0; j < items.size(); j++){
                            Map item = (Map) items.get(j);
                            // 商品id
                            String itemId = item.get("itemId").toString();
                            // 定义订单对象
                            ErpGoodsOrder erpGoodsOrder = null;
                            try{
                                // id查询erp订单是否存在
                                erpGoodsOrder = selectBoOrderNoAndGoodsId(orderSn,itemId);
                            } catch (Exception e) {
                                callBackData = "查询异常,异常参数:订单号:"+orderSn+";商品id:"+itemId;
                                // 打印异常
                                e.printStackTrace();
                            }
                            if (erpGoodsOrder == null){
                                newOrderList.add(order);
                                callBackData += "订单号："+orderSn+";商品id："+itemId+";已执行订单库存同步操作;";
                            }else{
                                // 校验是否下发
                                OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(erpGoodsOrder.getId());
                                // 未下发则重新执行下单操作
                                if (orderExternalGoods == null){
                                    newOrderList.add(order);
                                    callBackData += "订单号："+orderSn+";商品id："+itemId+";订单未下发，重新执行下发流程;";
                                }else{
                                    callBackData += "订单号："+orderSn+";商品id："+itemId+";订单已下发;";
                                }
                            }
                        }
                    }
                    runningTask.setCallBackData(callBackData);
                    runningTaskList.add(runningTask);
                }

                if (runningTaskList.size() > 0){
                    runningTaskService.batchInsert(runningTaskList);
                }

                // 获取总页数
                int pages = (int) successResponse.get("pages");
                // 如果当前页数大于等于总页数
                if (pageNum >= pages){
                    // 结束循环
                    break;
                }else{
                    // 页数++
                    pageNum++;
                }
            }else{
                // 解析错误信息
                Map errorResponse = (Map) resultMap.get("errorResponse");
                String tokenErrorCode = "1000,1001,2000,2001";
                if (isRefreshToken){
                    // 如果 isRefreshToken = true 代表token已经刷新，但依旧未查询到列表信息
                    break;
                }else if (tokenErrorCode.contains(errorResponse.get("code").toString())) {
                    // 刷新token
                    shop.setToken(tokenUtils.refreshToken(shop.getRefreshToken(), shop.getId()));
                    // 代表token已刷新
                    isRefreshToken = true;
                }else{
                    // 如果不是token过期的错误码则直接结束循环
                    break;
                }
            }
        }

        // 调用接口
        if (!newOrderList.isEmpty()){
            kfzOrderPush(shop,newOrderList,manua);
        }
    }

    @Override
    public int pddOrderPush(Message message,Boolean manua){
        Shop shop = shopService.selectShopByMallId(message.getMallID()+"");
        if(shop != null){
            // 在这里处理接收到的消息
            System.out.println("订单消息: " + message);
            Map contentMap = JsonUtil.transferToObj(message.getContent(),Map.class);
            String orderSn = contentMap.get("tid").toString();
            String resultStr = InterfaceUtils.getInterface("http://pdd.buzhiyushu.cn","/api/pdd/auth/getOrderDetail?accessToken="+shop.getToken()+"&orderSn="+orderSn);
            Map resultMap = JsonUtil.transferToObj(resultStr,Map.class);
            Map orderInfoGetResponse  = (Map) resultMap.get("order_info_get_response");
            Map orderInfo = null;
            try{
                orderInfo = (Map) orderInfoGetResponse.get("order_info");
            } catch (Exception e){
                System.out.println("解析订单商品信息异常："+e.getMessage());
                return 0;
            }
            Long orderStatus = OrderUtils.pddGetOrderStatus(Integer.parseInt(orderInfo.get("order_status").toString()));
            // 解析省市区
            String province = "";
            String city = "";
            String country = "";
            String town = "";
            // 收件人
            String receiverName = "";
            String mobile = "";

            if(StringUtils.isNotEmpty(orderInfo.get("address").toString())){
                List<PddOpenDecryptBatchRequest.DataListItem> dataList = new ArrayList<PddOpenDecryptBatchRequest.DataListItem>();
                PddOpenDecryptBatchRequest.DataListItem dataListItem = PddUtil.getDataListItem(orderSn,orderInfo.get("address").toString());
                dataList.add(dataListItem);
                dataListItem = PddUtil.getDataListItem(orderSn,orderInfo.get("receiver_address").toString());
                dataList.add(dataListItem);
                dataListItem = PddUtil.getDataListItem(orderSn,orderInfo.get("receiver_name").toString());
                dataList.add(dataListItem);
                dataListItem = PddUtil.getDataListItem(orderSn,orderInfo.get("receiver_phone").toString());
                dataList.add(dataListItem);
                try{
                    List response = PddUtil.decryptBatch(shop.getToken(),dataList);
                    for (Object object : response){
                        Map map = (Map) object;
                        if(Integer.parseInt(map.get("dataType").toString()) == 7){
                            String address = map.get("decryptedData").toString();
                            Map<String, String> addressInfo = AddressParserUtils.parseOriginalFormat(address);
                            province = addressInfo.get("province");
                            city = addressInfo.get("city");
                            country = addressInfo.get("country"); // 这是区县
                            town = addressInfo.get("town") + addressInfo.get("detail");
                        }else if (Integer.parseInt(map.get("dataType").toString()) == 5){
                            // 收件人
                            try{
                                receiverName = map.get("decryptedData").toString();
                            }catch (Exception e){
                                System.out.println("收件人解析异常："+e.getMessage());
                            }

                        }else if (Integer.parseInt(map.get("dataType").toString()) == 6){
                            // 手机号
                            try{
                                mobile = map.get("decryptedData").toString();
                            }catch (Exception e){
                                System.out.println("手机号解析异常："+e.getMessage());
                            }
                        }
                    }
                }catch (Exception e){
                    System.out.println( "解析订单地址异常："+e.getMessage());
                }
            }
            List<Map> itemList = (List<Map>) orderInfo.get("item_list");
            for(Map item : itemList){
                // 解析商品信息
                GoodsDto goodsDto = new GoodsDto();
                // 商品编号
                goodsDto.setGoodsId(item.get("goods_id").toString());
                // 商品名称
                goodsDto.setGoodsName(item.get("goods_name").toString());
                // 商品数量
                goodsDto.setGoodsCount(item.get("goods_count").toString());
                // 商品销售价格
                goodsDto.setGoodsPrice(new BigDecimal(item.get("goods_price").toString()).multiply(BigDecimal.valueOf(100)).toString());
                // 商品规格
                goodsDto.setGoodsSpec( item.get("goods_spec").toString());
                // 商品图片
                List<String> goodsImgs = new ArrayList<>();
                goodsImgs.add( item.get("goods_img").toString());
                goodsDto.setGoodsImgs(goodsImgs);
                // 商家外部编码
                goodsDto.setOuterGoodsId( item.get("outer_goods_id").toString());
                // 闲管家商品id
                goodsDto.setOuterId( item.get("outer_id").toString());
                // 规格编码
                goodsDto.setSkuId( item.get("sku_id").toString());
                // 根据订单号和商品id查询erp订单是否存在
                ErpGoodsOrder erpGoodsOrder = selectBoOrderNoAndGoodsId(orderSn,goodsDto.getGoodsId());
                // erpGoodsOrder 非null判断
                if(erpGoodsOrder == null){
                    erpGoodsOrder = new ErpGoodsOrder();
                    // 订单编号
                    erpGoodsOrder.setOrderSn(orderSn);
                    // 分类
                    erpGoodsOrder.setCatId1(Long.parseLong(orderInfo.get("cat_id_1").toString()));
                    erpGoodsOrder.setCatId2(Long.parseLong(orderInfo.get("cat_id_2").toString()));
                    erpGoodsOrder.setCatId3(Long.parseLong(orderInfo.get("cat_id_3").toString()));
                    erpGoodsOrder.setCatId4(Long.parseLong(orderInfo.get("cat_id_4").toString()));
                    // 平台店铺id
                    erpGoodsOrder.setShopId(shop.getMallId()+"");
                    // 店铺类型
                    erpGoodsOrder.setShopType(Long.parseLong(shop.getShopType()));
                    // erp店铺id
                    erpGoodsOrder.setShopErpId(shop.getId());
                    // erp店铺名称
                    erpGoodsOrder.setShopErpName(shop.getShopName());
                    // 订单中商品sku列表json字符串
                    erpGoodsOrder.setItemList(JsonUtil.transferToJson(goodsDto));
                    //订单价格（支付价格）
                    Long payAmount = new BigDecimal(getStringValue(orderInfo, "pay_amount")).multiply(BigDecimal.valueOf(100)).longValue();
                    erpGoodsOrder.setOrderTotal(payAmount);
                    // 支付金额 (以分为单位)
                    erpGoodsOrder.setPayAmount(payAmount);
                    // 折扣金额
                    erpGoodsOrder.setOrderChangeAmount(new BigDecimal(getStringValue(orderInfo, "discount_amount")).multiply(BigDecimal.valueOf(100)).longValue());
                    // 商品金额（以分为单位）商品金额=商品销售价格*商品数量-订单改价折扣金额
                    erpGoodsOrder.setGoodsAmount(new BigDecimal(getStringValue(orderInfo, "goods_amount")).multiply(BigDecimal.valueOf(100)).longValue());
                    // 创建人
                    erpGoodsOrder.setCreatedBy(Long.parseLong(shop.getCreateBy()));
                    // 创建时间 时间戳
                    erpGoodsOrder.setCreatedAt(TimestampConverter.toTimestamp(orderInfo.get("created_time").toString()));
                    // 是否可视化
                    erpGoodsOrder.setIsShow(0L);
                    // 是否下发订单 0 否 1 是
                    erpGoodsOrder.setIsIssue(0L);
                }

                // 订单中商品sku列表对象
                erpGoodsOrder.setGoodsDto(goodsDto);
                // 修改用，修改前的订单状态
                erpGoodsOrder.setOldOrderStatus(erpGoodsOrder.getOrderStatus());
                // 订单类型转换
                erpGoodsOrder.setOrderStatus(orderStatus);
                // 支付单号
                erpGoodsOrder.setPayNo(getStringValue(orderInfo, "pay_no"));
                // 支付时间 时间戳
                erpGoodsOrder.setPayAt(TimestampConverter.toTimestamp(orderInfo.get("pay_time").toString()));
                // 成交状态 0 未成交 1 成交
                erpGoodsOrder.setConfirmStatus(Long.parseLong(orderInfo.get("confirm_status").toString()));
                // 成交时间 时间戳
                erpGoodsOrder.setConfirmAt(TimestampConverter.toTimestamp(orderInfo.get("confirm_time").toString()));
                // 发货时间 时间戳
                erpGoodsOrder.setShippingAt(TimestampConverter.toTimestamp(orderInfo.get("shipping_time").toString()));
                // 如果省为空再进行获取地址信息
                if (StringUtils.isEmpty(erpGoodsOrder.getProvince())) {
                    erpGoodsOrder.setProvince(province);
                    erpGoodsOrder.setCity(city);
                    erpGoodsOrder.setCountry(country);
                    erpGoodsOrder.setTown(town);
                    erpGoodsOrder.setReceiverName(receiverName);
                    erpGoodsOrder.setMobile(mobile);
                }
                // 快递单号
                erpGoodsOrder.setTrackingNumber(getStringValue(orderInfo, "tracking_number"));
                // 修改前售后状态
                erpGoodsOrder.setOldAfterSalesStatus(erpGoodsOrder.getAfterSalesStatus());
                // 售后状态
                erpGoodsOrder.setAfterSalesStatus(getLongValue(orderInfo, "after_sales_status"));
                // 订单最近一次更新时间
                erpGoodsOrder.setUpdatedAt(TimestampConverter.toTimestamp(orderInfo.get("updated_at").toString()));

                // 订单具体操作方法
                orderOperation(shop,erpGoodsOrder,manua);
            }
            System.out.println("订单推送消息："+message);
        }
        return 0;
    }


    /**
     * 拼多多其他相关消息
     */
    @Override
    public void pddOtherMessage(Message message){
        Shop shop = shopService.selectShopByMallId(message.getMallID()+"");
        if(shop != null){
            Map contentMap = JsonUtil.transferToObj(message.getContent(),Map.class);
            String orderSn = contentMap.get("tid").toString();
            List<ErpGoodsOrder> erpGoodsOrderList = selectListByOrderNo(orderSn);
            if (!erpGoodsOrderList.isEmpty()){
                for (ErpGoodsOrder erpGoodsOrder : erpGoodsOrderList){
                    if (message.getType().equals("pdd_trade_TradeMemoModified")){
                        // 交易备注修改消息
                        erpGoodsOrder.setRemark(contentMap.get("seller_memo").toString());
                    }else if (message.getType().equals("pdd_trade_BuyerMemoModified")){
                        // 买家备注修改消息
                        erpGoodsOrder.setBuyerMemo(contentMap.get("buyer_memo").toString());
                    }
                    update(erpGoodsOrder);
                }
            }
        }
    }

    /**
     * 拼多多审核驳回消息
     */
    @Override
    public void pddReviewRejected(Message message){
        // 处理商品审核驳回消息的具体逻辑
        // 判断消息文本内容是否为空
        if (ObjectUtil.isEmpty(message.getContent())) return;
        // 解析消息文本内容
        GoodsCheckRejectVo goodsCheckRejectVo = JSONObject.parseObject(message.getContent(), GoodsCheckRejectVo.class);
        Assert.isTrue(!ObjectUtil.isEmpty(message.getContent()), "消息内容解析失败" + JSONObject.toJSONString(message));
        if (goodsCheckRejectVo.getRejectComment().contains("类目")){
            shopGoodsPublishedClient.uodateCatId("https://api.buzhiyushu.cn", "1", goodsCheckRejectVo.getMallId(), goodsCheckRejectVo.getGoodsId().toString());
        }
        try {
            shopGoodsPublishedClient.delShopGoodsPublished("https://api.buzhiyushu.cn", "1", goodsCheckRejectVo.getMallId(), goodsCheckRejectVo.getGoodsId().toString(),goodsCheckRejectVo.getRejectComment());
        } catch (Exception e) {
            throw new RuntimeException("接收到pdd商品审核驳回消息删除发布商品失败:" + e);
        }
    }

    /**
     *  消息插入redis
     */
    @Override
    public void messageSetRedis(Message message){
        if (message.getMallID() == 783762954 || message.getMallID() == 922420730){
            System.out.println("商品操作消息:"+ message);
            String type = message.getType();
            Shop shop = shopService.selectShopByMallId(message.getMallID()+"");
            if (shop != null){
                Map map = new HashMap();
                map.put("type",type);
                map.put("mallId",message.getMallID().toString());
                Map contentMap = JsonUtil.transferToObj(message.getContent(),Map.class);
                map.put("goodsId",contentMap.get("goods_id").toString());
                if (type.equals("pdd_goods_GoodsCheckReject")){
                    // 商品驳回存储原因和时间
                    map.put("goodsCommitId",contentMap.get("goods_commit_id") == null ? "" : contentMap.get("goods_commit_id").toString());
                    map.put("rejectComment",contentMap.get("reject_comment") == null ? "" : contentMap.get("reject_comment").toString());
                    map.put("rejectTime",contentMap.get("reject_time") == null ? "" : contentMap.get("reject_time").toString());
                }
                redisService.listSetOrAppendAsJson(shop.getId().toString(),map);
            }
        }
    }

    /**
     * 订单具体操作方法
     * @param shop              店铺信息
     * @param erpGoodsOrder     订单信息
     */
    public void orderOperation(Shop shop, ErpGoodsOrder erpGoodsOrder,Boolean manua){
        // 防止多条查询，获取第一条
        WarehouseSettings warehouseSettingsVo = getWarehouseSettings(Long.parseLong(shop.getCreateBy()));
        if (erpGoodsOrder.getId() == null){
            // 新增订单
            insert(erpGoodsOrder);
            // 如果是新增订单,但类型不是待付款的订单，扣减库存
            if (erpGoodsOrder.getOrderStatus() != 1){
                manua = true;
            }
            // 首次获取其他类型的订单，记录日志
            String log = "订单新增-订单编号：" + erpGoodsOrder.getOrderSn() + ";"
                    +"订单类型："+ OrderUtils.xyGetOrderStatusTxt(erpGoodsOrder.getOrderStatus())+";"
                    +"售后状态："+ OrderUtils.xyGetAfterSalesStatusTxt(erpGoodsOrder.getAfterSalesStatus())+";";
            // 记录日志
            Boolean bool = OrderUtils.addToOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
            if(!bool){
                // 记录日志错误则创建日志
                OrderUtils.createOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
            }
        }else{
            // 修改订单
            update(erpGoodsOrder);
            if (!manua){
                // 订单修改时触发的操作
                editOrder(erpGoodsOrder,shop,erpGoodsOrder.getGoodsDto(),warehouseSettingsVo);
            }
        }
        // 如果设置表为空，或者库存同步形式是 支付减库存，则 ifOrderStatus = 2
        Long ifOrderStatus = warehouseSettingsVo == null || warehouseSettingsVo.getStockSynchronizeType() == 1 ? 2L : 1L;
        // 校验订单状态是否与设置模板中库存同步设置一致并且不存在售后
        if((erpGoodsOrder.getOrderStatus() == 2L && erpGoodsOrder.getAfterSalesStatus() == 0L) || manua){
            // 新增或已付款未发货的订单触发的操作 新增订单
            addOrderIssue(erpGoodsOrder,shop,erpGoodsOrder.getGoodsDto(),warehouseSettingsVo,manua);
        }else if (erpGoodsOrder.getOrderStatus() != 1 && erpGoodsOrder.getAfterSalesStatus() != 2){
            // 不下发的订单代表是其他类型的，修改状态已下发
            erpGoodsOrder.setIsIssue(1L);
            // 修改订单下发状态
            update(erpGoodsOrder);
        }
    }



    /**
     * 获取匹配规则
     * @param createBy  创建人
     * @return
     */
    public WarehouseSettings getWarehouseSettings(Long createBy){
        // 查询规则表数据
        WarehouseSettings warehouseSettingsBo = new WarehouseSettings();
        // 分页第几页
        warehouseSettingsBo.setPageNum(1);
        // 分页每页多少条
        warehouseSettingsBo.setPageSize(10);
        // 删除标记 0 未删除 1 已删除
        warehouseSettingsBo.setDelFlag(0L);
        // 启用状态 0 禁用 1 启用
        warehouseSettingsBo.setStatus(1L);
        // 设置模板的创建人
        warehouseSettingsBo.setCreateBy(createBy);
        // 获取设置参数模板
        List<WarehouseSettings> warehouseSettingsList = warehouseSettingsService.getPageList(warehouseSettingsBo);
        // 防止多条查询，获取第一条
        WarehouseSettings warehouseSettingsVo = warehouseSettingsList.isEmpty() ? null : warehouseSettingsList.get(0);
        // 非null判断
        if(warehouseSettingsVo != null){
            // 获取模板内的匹配规则
            List<UserSettingsAttribute> userSettingsAttributeList = userSettingsAttributeService.selectByWarehouseSettingsId(warehouseSettingsVo.getId());
            // 存入设置模板对象中
            warehouseSettingsVo.setUserSettingsAttributeList(userSettingsAttributeList);
        }

        return warehouseSettingsVo;
    }


    /**
     * 获取新增的ErpGoodsOrder对象
     *
     * @param orderNo           订单号
     * @param shop              店铺信息
     * @param goodsDto          商品信息
     * @param orderDetailDataMap 订单明细信息
     * @return 填充后的ErpGoodsOrder对象
     */
    public ErpGoodsOrder getAddErpGoodsOrder(String orderNo, Shop shop, GoodsDto goodsDto, Map orderDetailDataMap) {
        // 订单状态 orderStatus：
        //  11 待付款
        //  12 待发货
        //  21 已发货
        //  22 交易成功
        //  23 已退款
        //  24 交易关闭
        Integer orderStatus = (Integer) orderDetailDataMap.get("order_status");
        // 售后状态 afterSalesStatus:
        //  2   待商家处理
        //  4   待买家退货
        //  603 待商家收货
        //  11  退款关闭
        //  4   退款成功
        //  6   已拒绝退款
        //  604 待确认退货地址
        Integer refundStatus = (Integer) orderDetailDataMap.get("refund_status");
        Long afterSalesStatus = OrderUtils.getAfterSalesStatus(refundStatus);
        // 新增订单对象
        ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
        // 订单编号
        erpGoodsOrder.setOrderSn(orderNo);
        // 平台店铺id
        erpGoodsOrder.setShopId(shop.getShopKey());
        // 店铺类型
        erpGoodsOrder.setShopType(Long.parseLong(shop.getShopType()));
        // erp店铺id
        erpGoodsOrder.setShopErpId(shop.getId());
        // erp店铺名称
        erpGoodsOrder.setShopErpName(shop.getShopName());
        // 售后状态
        erpGoodsOrder.setAfterSalesStatus(afterSalesStatus);
        // 订单中商品sku列表json字符串
        erpGoodsOrder.setItemList(JsonUtil.transferToJson(goodsDto));
        // 订单中商品sku列表对象
        erpGoodsOrder.setGoodsDto(goodsDto);
        // 订单金额 单位：分
        erpGoodsOrder.setOrderTotal(getLongValue(orderDetailDataMap, "total_amount"));
        // 支付金额 (以分为单位)
        erpGoodsOrder.setPayAmount(getLongValue(orderDetailDataMap, "pay_amount"));
        // 创建人
        erpGoodsOrder.setCreatedBy(Long.parseLong(shop.getCreateBy()));
        // 创建时间 时间戳
        erpGoodsOrder.setCreatedAt(getLongValue(orderDetailDataMap, "create_time"));
        // 支付单号
        erpGoodsOrder.setPayNo(getStringValue(orderDetailDataMap, "pay_no"));
        // 支付时间 时间戳
        erpGoodsOrder.setPayAt(getLongValue(orderDetailDataMap, "pay_time"));
        //校验订单状态，22是已成交
        if (orderStatus == 22) {
            // 成交状态 0 未成交 1 成交
            erpGoodsOrder.setConfirmStatus(1L);
            // 成交时间 时间戳
            erpGoodsOrder.setConfirmAt(getLongValue(orderDetailDataMap, "confirm_time"));
        } else {
            erpGoodsOrder.setConfirmStatus(0L);
        }
        // 订单类型转换
        erpGoodsOrder.setOrderStatus(OrderUtils.xyGetOrderStatus(orderStatus));
        // 发货时间 时间戳
        erpGoodsOrder.setShippingAt(getLongValue(orderDetailDataMap, "consign_time"));
        // 收件地省份
        erpGoodsOrder.setProvince(getStringValue(orderDetailDataMap, "prov_name"));
        // 收件地城市
        erpGoodsOrder.setCity(getStringValue(orderDetailDataMap, "city_name"));
        // 收件地国家或地区
        erpGoodsOrder.setCountry(getStringValue(orderDetailDataMap, "area_name"));
        // 收件地区县
        erpGoodsOrder.setTown(getStringValue(orderDetailDataMap, "town_name"));
        // 收件人
        erpGoodsOrder.setReceiverName(getStringValue(orderDetailDataMap, "receiver_name"));
        // 收件人号码
        erpGoodsOrder.setMobile(getStringValue(orderDetailDataMap, "receiver_mobile"));
        // 快递单号 tracking_number
        erpGoodsOrder.setTrackingNumber(getStringValue(orderDetailDataMap, "waybill_no"));
        // 商家订单备注
        erpGoodsOrder.setRemark(getStringValue(orderDetailDataMap, "seller_remark"));
        // 售后状态
        erpGoodsOrder.setAfterSalesStatus(afterSalesStatus);
        // 订单最近一次更新时间
        erpGoodsOrder.setUpdatedAt(getLongValue(orderDetailDataMap, "update_time"));
        // 是否可视化
        erpGoodsOrder.setIsShow(0L);
        // 是否下发订单 0 否 1 是
        erpGoodsOrder.setIsIssue(0L);
        return erpGoodsOrder;
    }

    /**
     * 获取编辑的ErpGoodsOrder对象
     *
     * @param erpGoodsOrder 原始订单对象
     * @param goodsDto 商品信息
     * @param orderDetailDataMap 订单明细信息
     * @return 更新后的ErpGoodsOrder对象
     */
    public ErpGoodsOrder getEditErpGoodsOrder(ErpGoodsOrder erpGoodsOrder, GoodsDto goodsDto, Map orderDetailDataMap) {

        // 订单状态 orderStatus：
        //  11 待付款
        //  12 待发货
        //  21 已发货
        //  22 交易成功
        //  23 已退款
        //  24 交易关闭
        Integer orderStatus = (Integer) orderDetailDataMap.get("order_status");
        // 订单退款状态
        Integer refundStatus = (Integer) orderDetailDataMap.get("refund_status");
        // 售后状态 afterSalesStatus:
        //  2   待商家处理
        //  4   待买家退货
        //  603 待商家收货
        //  11  退款关闭
        //  4   退款成功
        //  6   已拒绝退款
        //  604 待确认退货地址
        Long afterSalesStatus = OrderUtils.getAfterSalesStatus(refundStatus);
        // 订单中商品sku列表
        erpGoodsOrder.setGoodsDto(goodsDto);
        // 修改用，修改前的订单状态
        erpGoodsOrder.setOldOrderStatus(erpGoodsOrder.getOrderStatus());
        // 订单类型转换
        erpGoodsOrder.setOrderStatus(OrderUtils.xyGetOrderStatus(orderStatus));
        // 修改前售后状态
        erpGoodsOrder.setOldAfterSalesStatus(erpGoodsOrder.getAfterSalesStatus());
        // 售后状态
        erpGoodsOrder.setAfterSalesStatus(afterSalesStatus);
        // 支付单号
        erpGoodsOrder.setPayNo(getStringValue(orderDetailDataMap, "pay_no"));
        // 支付时间 时间戳
        erpGoodsOrder.setPayAt(getLongValue(orderDetailDataMap, "pay_time"));
        // 成交状态 0 未成交 1 成交
        if (orderStatus == 22) {
            erpGoodsOrder.setConfirmStatus(1L);
            // 成交时间 时间戳
            erpGoodsOrder.setConfirmAt(getLongValue(orderDetailDataMap, "confirm_time"));
        } else {
            erpGoodsOrder.setConfirmStatus(0L);
        }
        // 发货时间 时间戳
        erpGoodsOrder.setShippingAt(getLongValue(orderDetailDataMap, "consign_time"));
        // 如果省为空再进行获取地址信息
        if (StringUtils.isEmpty(erpGoodsOrder.getProvince())) {
            // 收件地省份
            erpGoodsOrder.setProvince(getStringValue(orderDetailDataMap, "prov_name"));
            // 收件地城市
            erpGoodsOrder.setCity(getStringValue(orderDetailDataMap, "city_name"));
            // 收件地国家或地区
            erpGoodsOrder.setCountry(getStringValue(orderDetailDataMap, "area_name"));
            // 收件地区县
            erpGoodsOrder.setTown(getStringValue(orderDetailDataMap, "town_name"));
            // 收件人
            erpGoodsOrder.setReceiverName(getStringValue(orderDetailDataMap, "receiver_name"));
            // 收件人号码
            erpGoodsOrder.setMobile(getStringValue(orderDetailDataMap, "receiver_mobile"));
        }
        // 快递单号
        erpGoodsOrder.setTrackingNumber(getStringValue(orderDetailDataMap, "waybill_no"));
        // 商家订单备注
        erpGoodsOrder.setRemark(getStringValue(orderDetailDataMap, "seller_remark"));
        // 订单最近一次更新时间
        erpGoodsOrder.setUpdatedAt(getLongValue(orderDetailDataMap, "update_time"));
        return erpGoodsOrder;
    }

    /**
     * 从Map中获取Long类型值，为空时返回0L
     *
     * @param map 数据Map
     * @param key 键名
     * @return Long类型值，为空时返回0L
     */
    private Long getLongValue(Map map, String key) {
        Object value = map.get(key);
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    /**
     * 从Map中获取String类型值，为空时返回空字符串
     *
     * @param map 数据Map
     * @param key 键名
     * @return String类型值，为空时返回空字符串
     */
    private String getStringValue(Map map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString();
    }

    /**
     * 修改订单时的操作
     * @param erpGoodsOrder     订单信息
     * @param shop              店铺信息
     * @param goodsDto          商品信息
     */
    public void editOrder(ErpGoodsOrder erpGoodsOrder,Shop shop,GoodsDto goodsDto,WarehouseSettings warehouseSettings){
        /**
         * 修改erp订单信息
         */
        String log = "订单更新-订单编号：" + erpGoodsOrder.getOrderSn() + ";"
                +"订单类型："+ OrderUtils.xyGetOrderStatusTxt(erpGoodsOrder.getOrderStatus())+";"
                +"售后状态："+ OrderUtils.xyGetAfterSalesStatusTxt(erpGoodsOrder.getAfterSalesStatus())+";";

        // 查询是否已经存在记录
        OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderIdAndIsDistribution(erpGoodsOrder.getId(),"0");
        // 如果之前的订单处于未发货状态，并且设置模板为空或者设置模板中是否自动回复库存=1时，执行库存回退操作
        if(erpGoodsOrder.getOldOrderStatus() < 3L  && erpGoodsOrder.getAfterSalesStatus() == 10L && (warehouseSettings == null || warehouseSettings.getStockRollback() == 1)){
            // 订单下单数量
            int quantity = Integer.parseInt(goodsDto.getGoodsCount());
            // 关联的商品不为空并且是外部订单时。 orderExternalGoods.getType() 1 是内部订单  2是外部订单
            if(orderExternalGoods != null && orderExternalGoods.getType() != null && orderExternalGoods.getType() == 2){
                System.out.println("外部订单执行了回退库存操作");
                // 获取商品信息根据erp商品id
                ZhishuShopGoods zhishuShopGoods = zhishuShopGoodsService.selectById(orderExternalGoods.getGoodsId());
                // 增加后的库存
                int inventory = Integer.parseInt(zhishuShopGoods.getInventory().toString()) + quantity;
                // 执行修改库存以及库存同步操作
                synchronizeStock(zhishuShopGoods.getId(),inventory,Integer.parseInt(zhishuShopGoods.getInventory().toString()),shop.getCreateBy(),"1",erpGoodsOrder.getId());
                // 退款金额
                BigDecimal total =  rollbackPrice( orderExternalGoods,shop);
                log += "订单处于未发货状态，买家进行了退款。进行回退库存操作并执行了库存同步操作;原始库存：" + zhishuShopGoods.getInventory() + " 回退后库存："+inventory+";并进行了退款："+total.divide(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP) + "元";
            }else if (orderExternalGoods != null && orderExternalGoods.getType() != null && orderExternalGoods.getType() == 1){
                System.out.println("内部订单执行了回退库存操作");
                // 获取已发布商品数据
                String goodsId = "-1";
                if(shop.getShopType().equals("5")){
                    goodsId = goodsDto.getOuterId();        //闲鱼
                }else if(shop.getShopType().equals("1") || shop.getShopType().equals("2")){
                    goodsId = goodsDto.getGoodsId();        //拼多多
                }
                ShopGoodsPublished shopGoodsPublished = shopGoodsPublishedService.selectByShopIdAndPlatformId(shop.getId().toString(),goodsId);
                //增加后的库存
                int inventory = shopGoodsPublished.getInventory() + quantity;
                //执行修改库存以及库存同步操作
                synchronizeStock(shopGoodsPublished.getShopGoodsId(),inventory,shopGoodsPublished.getInventory(),shop.getCreateBy(),"1",erpGoodsOrder.getId());
                log += "订单处于未发货状态，买家进行了退款。进行回退库存操作并执行了库存同步操作;原始库存：" + shopGoodsPublished.getInventory() + " 回退后库存："+inventory;
            }else{
                log += "订单处于未下发状态，买家进行了退款";
            }
        }else if(erpGoodsOrder.getOrderStatus() == 4L && (erpGoodsOrder.getAfterSalesStatus() == 0L || erpGoodsOrder.getAfterSalesStatus() == 11L)){
            /**
             * 订单处于交易完成并且无售后的情况
             * 1、根据订单id查询 “外部订单关联的订单与erp商品表”
             * 2、存在数据表示市外部订单，外部订单需要进行分润 从给仓库的钱扣除手续费后给仓库账户
             */
            if(orderExternalGoods != null && orderExternalGoods.getType() == 2){
                System.out.println("执行了外部订单分润");
                // 获取应该给仓库的金额
                BigDecimal payPrice = new BigDecimal(orderExternalGoods.getPayPrice());
                // 从仓库不可用金额转入仓库可用金额
                String result = InterfaceUtils.getInterfacePost("https://api.buzhiyushu.cn",
                        "/system/user/balance/transfer/freezeToBalance?fromUserId="+orderExternalGoods.getDeptUseId()+"&toUserId="+orderExternalGoods.getDeptUseId()+"&amount="+payPrice,new HashMap<>());
                System.out.println("仓库钱包转仓库可用金额结果："+result);
                // 生成仓库分销订单
                Map profitsharingFormData = new HashMap();
                // 订单类型
                profitsharingFormData.put("orderType","warehouseOrder");
                // 订单id
                profitsharingFormData.put("Id",erpGoodsOrder.getId().toString());
                // 订单编号
                profitsharingFormData.put("OrderSn",erpGoodsOrder.getOrderSn());
                // 出账用户id
                profitsharingFormData.put("UserId",orderExternalGoods.getDeptUseId());
                // 支付金额
                profitsharingFormData.put("PayAmount",payPrice.toString());
                // 总金额
                profitsharingFormData.put("TotalAmount",payPrice.toString());
                // 商品数量
                profitsharingFormData.put("Count",erpGoodsOrder.getGoodsDto().getGoodsCount());
                // 商品id
                profitsharingFormData.put("GoodsID",erpGoodsOrder.getGoodsDto().getGoodsId());
                // 调用接口,创建分润订单
                System.out.println("仓库分润接口调用参数："+profitsharingFormData);
                String profitsharingResult = InterfaceUtils.postForm("http://119.45.237.193:9096","/api/profitsharing/insertOrderBycfg",profitsharingFormData);
                System.out.println("仓库分润接口调用结果："+profitsharingResult);
                // 转义调用接口返回值
                Map profitsharingResultMap = JsonUtil.transferToObj(profitsharingResult,Map.class);
                // 如果 message 为 success说明分润成功
                if(profitsharingResultMap.get("message").equals("success") && profitsharingResultMap.get("data") != null){
                    // 解析返回值中的data参数
                    Map profitsharingData = (Map) profitsharingResultMap.get("data");
                    // 创建记录出入帐任务id的对象
                    ErpGoodsOrderAccounts erpGoodsOrderAccounts = new ErpGoodsOrderAccounts();
                    // erp订单id
                    erpGoodsOrderAccounts.setErpOrderId(erpGoodsOrder.getId());
                    // 出账id
                    erpGoodsOrderAccounts.setGetId(Long.parseLong(profitsharingData.get("get_id").toString()));
                    // 出账状态
                    erpGoodsOrderAccounts.setGetStatus(0);
                    // 入账id
                    erpGoodsOrderAccounts.setSetId(Long.parseLong(profitsharingData.get("set_id").toString()));
                    // 入账装填
                    erpGoodsOrderAccounts.setSetStatus(0);
                    // 执行新增
                    erpGoodsOrderAccountsService.save(erpGoodsOrderAccounts);
                }
                //修改 order_external_goods 表 is_distribution  = 1
                orderExternalGoods.setIsDistribution("1");
                orderExternalGoodsService.update(orderExternalGoods);
                log += "订单交易完成;外部商品已执行分润操作";
            }
        }else if(erpGoodsOrder.getOldOrderStatus() >= 3L  && erpGoodsOrder.getAfterSalesStatus() == 2L && orderExternalGoods != null && orderExternalGoods.getType() == 2){
            //当订单状态是已发货并且售后状态是待商家处理并且是外部订单的情况下，发送订单给仓库
            erpGoodsOrder.setErpAfterSalesStatus(1L);
            erpGoodsOrder.setErpAssCreateAt(System.currentTimeMillis() / 1000);
            update(erpGoodsOrder);
        }
        //记录日志
        Boolean bool = OrderUtils.addToOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
        if(!bool){
            //记录日志错误则创建日志
            OrderUtils.createOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
        }
    }

    /**
     * 回滚费用
     */
    @Override
    public BigDecimal rollbackPrice(OrderExternalGoods orderExternalGoods, Shop shop){
        // 获取应该给仓库的金额
        BigDecimal payPrice = new BigDecimal(orderExternalGoods.getPayPrice());
        // 获取支付给系统的手续费
        BigDecimal serviceCharge = new BigDecimal(orderExternalGoods.getServiceCharge());
        // 获取总支付金额
        BigDecimal total = payPrice.add(serviceCharge);
        // 从仓库钱包不可用金额转入店铺可用金额
        String result = InterfaceUtils.getInterfacePost("https://api.buzhiyushu.cn",
                "/system/user/balance/transfer/freezeToBalance?fromUserId="+orderExternalGoods.getDeptUseId()+"&toUserId="+shop.getCreateBy()+"&amount="+payPrice,new HashMap<>());
        System.out.println("外部订单执行了回退库存操作："+result);
        // 回滚手续费 获取账目数据id
        List<ErpGoodsOrderAccounts> erpGoodsOrderAccountsList = erpGoodsOrderAccountsService.getByOrderId(orderExternalGoods.getOrderId());
        // 循环处理
        for (ErpGoodsOrderAccounts erpGoodsOrderAccounts : erpGoodsOrderAccountsList){
            System.out.println("手续费回滚");
            // 调用接口
            Map rollbackAccountMap = new HashMap();
            // 出账账目id
            rollbackAccountMap.put("access_id",erpGoodsOrderAccounts.getGetId());
            // 订单id
            rollbackAccountMap.put("about_id",erpGoodsOrderAccounts.getErpOrderId());
            // 调用接口
            InterfaceUtils.postForm("http://119.45.237.193:9096","/api/account/rollbackAccount",rollbackAccountMap);
            // 入账账目id
            rollbackAccountMap.put("access_id",erpGoodsOrderAccounts.getSetId());
            // 调用接口
            InterfaceUtils.postForm("http://119.45.237.193:9096","/api/account/rollbackAccount",rollbackAccountMap);
            // 出账处理状态  0 未处理  1 已处理
            erpGoodsOrderAccounts.setGetStatus(1);
            // 入账处理状态  0 未处理  1 已处理
            erpGoodsOrderAccounts.setSetStatus(1);
            erpGoodsOrderAccountsService.update(erpGoodsOrderAccounts);
        }

        return total;
    }

    /**
     * 新增的订单下发
     * @param erpGoodsOrder     订单信息
     * @param shop              店铺信息
     * @param goodsDto          商品信息
     */
    public void addOrderIssue(ErpGoodsOrder erpGoodsOrder,Shop shop,GoodsDto goodsDto,WarehouseSettings warehouseSettings,Boolean manua){

        String log = "订单执行了下发订单操作;订单编号：" + erpGoodsOrder.getOrderSn() + ";"
                +"订单类型："+ OrderUtils.xyGetOrderStatusTxt(erpGoodsOrder.getOrderStatus())+";"
                +"售后状态："+ OrderUtils.xyGetAfterSalesStatusTxt(erpGoodsOrder.getAfterSalesStatus())+";";
        // 自动审核设置：默认自动审核
        Long autoIssue = warehouseSettings == null ? 1 : warehouseSettings.getAutoIssue();
        if(autoIssue == 1){
            // 下单数量
            int quantity = Integer.parseInt(goodsDto.getGoodsCount());
            // 根据店铺id和平台商品id查询已发布商品表

            String goodsId = "-1";
            if(shop.getShopType().equals("5")){
                goodsId = goodsDto.getOuterId();        //闲鱼
            }else if(shop.getShopType().equals("1") || shop.getShopType().equals("2")){
                goodsId = goodsDto.getGoodsId();        //拼多多
            }

            ShopGoodsPublished shopGoodsPublished = shopGoodsPublishedService.selectByShopIdAndPlatformId(shop.getId().toString(), goodsId);
            if (shopGoodsPublished == null && shop.getShopType().equals("2")){
                // 如果未查询到已发布记录并且是孔网订单，则根据货号查询
                log += "根据货号查询："+goodsDto.getOuterGoodsId()+";";
                try{
                    ZhishuShopGoods zhishuShopGoods = zhishuShopGoodsService.selectByArtNo(goodsDto.getOuterGoodsId());
                    if (zhishuShopGoods != null){
                        shopGoodsPublished = new ShopGoodsPublished();
                        shopGoodsPublished.setIsbn(zhishuShopGoods.getIsbn());
                        shopGoodsPublished.setInventory(Integer.parseInt(zhishuShopGoods.getInventory().toString()));
                        shopGoodsPublished.setShopGoodsId(zhishuShopGoods.getId().toString());
                        log += "查询成功;";
                    }else {
                        log += "查询失败;";
                    }
                }catch (Exception e){
                    System.out.println("根据货号查询商品异常");
                }
            }
            if(shopGoodsPublished != null){
                System.out.println("下单数量："+quantity+";库存数量："+shopGoodsPublished.getInventory()+";比对结果:"+(shopGoodsPublished.getInventory() >= quantity));
                // 库存充足的情况 校验订单类型，外部订单需要执行扣款，库存同步操作
                if(shopGoodsPublished.getInventory() >= quantity){
                    System.out.println("库存充足");
                    // 获取商品信息
                    ZhishuShopGoods zhishuShopGoodsVo = zhishuShopGoodsService.selectById(Long.parseLong(shopGoodsPublished.getShopGoodsId()));
                    // 校验订单类型，外部订单执行扣款，库存同步操作
                    log = externalOrderOperation(shop,erpGoodsOrder,zhishuShopGoodsVo,quantity,log,warehouseSettings,manua);
                }else{
                    // 将库存同步其他平台
                    int inventory = 0;
                    // 查看库存是为0  还是 大于0，如果是0，则同步0库存，如果大于0，则同步大于0的库存
                    if((quantity - shopGoodsPublished.getInventory()) > 0){
                        // 赋值自营书品库存
                        inventory = shopGoodsPublished.getInventory();
                    }
                    // 执行库存同步
                    synchronizeStock(shopGoodsPublished.getShopGoodsId(),inventory,shopGoodsPublished.getInventory(),shop.getCreateBy(),"2",erpGoodsOrder.getId());
                    // 只有在自动拉取的时候触发匹配别人仓库的商品
                    if (!manua){
                        // 根据规则查询匹配的仓库商品进行下发
                        List<ZhishuShopGoods> zhishuShopGoodsList = selectZhishuShopGoods(shopGoodsPublished.getIsbn(),Long.parseLong(quantity+"") ,shop,warehouseSettings,erpGoodsOrder);
                        // 查询是否存在匹配的商品
                        if(!zhishuShopGoodsList.isEmpty()){
                            ZhishuShopGoods zhishuShopGoodsVo = zhishuShopGoodsList.get(0);
                            // 执行扣款，库存同步操作
                            log = externalOrderOperation(shop,erpGoodsOrder,zhishuShopGoodsVo,quantity,log,warehouseSettings,manua);
                        }else{
                            log += "自营书品库存不足，并且根据仓库设置未匹配到其他仓库。需要进行手动下发";
                        }
                    }else{
                        log += "手动订单库存同步，自营书品库存不足,已同步各平台库存："+inventory;
                    }

                }
            }else{
                log += "未查询到相关记录。商品编号："+goodsId+";外部编码："+goodsDto.getOuterGoodsId();
            }
            // 记录日志
            Boolean bool = OrderUtils.addToOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
            if(!bool){
                // 记录日志错误则创建日志
                OrderUtils.createOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
            }
        }
    }


    /**
     * 库存不足时的操作
     * @param shop              店铺信息
     * @param erpGoodsOrder     订单信息
     * @param zhishuShopGoods   商品信息
     * @param quantity          库存
     * @param log               日志
     * @return
     */
    @Override
    public String externalOrderOperation(Shop shop,ErpGoodsOrder erpGoodsOrder,ZhishuShopGoods zhishuShopGoods,int quantity,String log,WarehouseSettings warehouseSettings,Boolean manua){
        // 如果查询出来的也是自己自营书品中的则不需要进行分销
        if(Long.parseLong(shop.getCreateBy()) != zhishuShopGoods.getUserId()) {
            if (manua){
                log += "手动订单库存同步,不进行额外分销功能";
                return log;
            }
            System.out.println("执行分销操作:外部订单操作;商品对象参数："+zhishuShopGoods);
            // 如果isbn优先则重新匹配商品
            if(warehouseSettings != null){
                // 循环设置匹配规则
                for(UserSettingsAttribute userSettingsAttribute : warehouseSettings.getUserSettingsAttributeList()){
                    // 判断是否是isbn优先，只有isbn优先，才重新匹配商品
                    if(userSettingsAttribute.getAttributeId() == 1 && userSettingsAttribute.getAttributeValue().equals("1")){
                        System.out.println("根据isbn优先匹配，重新获取商品");
                        // 外部订单根据设置重新匹配商品
                        List<ZhishuShopGoods> zhishuShopGoodsList = selectZhishuShopGoods(zhishuShopGoods.getIsbn(),Long.parseLong(quantity+"") ,shop,warehouseSettings,erpGoodsOrder);
                        // 匹配到了商品
                        if(!zhishuShopGoodsList.isEmpty()){
                            // 使用第一个匹配的商品
                            zhishuShopGoods = zhishuShopGoodsList.get(0);
                            System.out.println("重新获取的商品信息："+zhishuShopGoods);
                        }
                    }
                }
            }
            // 如果订单商品信息为空，则解析json字符串获取对象
            if(erpGoodsOrder.getGoodsDto() == null){
                // 解析erp订单中商品信息
                GoodsDto goodsDto = JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class);
                // 赋值
                erpGoodsOrder.setGoodsDto(goodsDto);
            }
            // 如果运费为空
            if(zhishuShopGoods.getShippingCost() == null){
                // 格式化省份字段内容
                String province = erpGoodsOrder.getProvince().replace("省","").replace("市","").replace("自治区","");
                // 获取运费
                BigDecimal shippingCost = getShippingCost(zhishuShopGoods.getDepotId(), province,quantity,shop);
                System.out.println("运费："+shippingCost);
                // 获取手续费百分比
                int percentageInt = getPercentageInt(shop.getShopType());
                // 将手续费转为小数
                BigDecimal percentage = new BigDecimal(percentageInt).divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
                // 获取手续费 （仓库卖价 + 运费） * 百分比 不保留小数
                BigDecimal serviceCharge = new BigDecimal(zhishuShopGoods.getPrice())
                        .add(shippingCost)
                        .multiply(percentage)
                        .setScale(0, RoundingMode.HALF_UP);
                System.out.println("手续费："+serviceCharge);
                // 赋值运费
                zhishuShopGoods.setShippingCost(shippingCost);
                // 赋值手续费
                zhishuShopGoods.setServiceCharge(serviceCharge);
                // 赋值总费用
                zhishuShopGoods.setTotalCost( new BigDecimal(zhishuShopGoods.getPrice()).add(shippingCost));
            }

            // 获取总费用  仓库卖价 + 运费 ，手续费通过分销接口获取
            BigDecimal totalPrice= new BigDecimal(zhishuShopGoods.getPrice()).add(zhishuShopGoods.getShippingCost());
            // 获取用户余额信息
            SysUser sysUser = userService.selectUserOne(Long.parseLong(shop.getCreateBy()));
            System.out.println("金额："+sysUser.getBalance()+";总费用："+(totalPrice.add(zhishuShopGoods.getServiceCharge()))+";校验结果："+(sysUser.getBalance().compareTo(totalPrice.add(zhishuShopGoods.getServiceCharge())) >= 0));
            // 判断sysUser.getBalance()是否大于shippingCost
            if(sysUser.getBalance().compareTo(totalPrice.add(zhishuShopGoods.getServiceCharge())) >= 0){
                System.out.println("金额充足："+sysUser.getBalance());
                // 金额充足 扣减金额到不可用资金中
                String result = InterfaceUtils.getInterfacePost("https://api.buzhiyushu.cn","/system/user/balance/transfer/balanceToFreeze?fromUserId="+shop.getCreateBy()+"&toUserId="+zhishuShopGoods.getUserId()+"&amount="+totalPrice,new HashMap());
                System.out.println("转账接口调用结果："+result);
                if (result.contains("转账成功")){
                    //修改金额成功后，增加外部订单关联的订单与erp商品表数据
                    System.out.println("执行了增加外部订单关联的订单与erp商品表数据操作");
                    // 添加分润订单
                    Map profitsharingFormData = new HashMap();
                    if(shop.getShopType().equals("1")){
                        //拼多多 赋值分润配置类型
                        profitsharingFormData.put("orderType","pddOrder");
                    } else if(shop.getShopType().equals("2")){
                        //孔夫子 赋值分润配置类型
                        profitsharingFormData.put("orderType","kfzOrder");
                    } else if (shop.getShopType().equals("5")) {
                        //咸鱼 赋值分润配置类型
                        profitsharingFormData.put("orderType","goofishOrder");
                    }
                    // erp订单id
                    profitsharingFormData.put("Id",erpGoodsOrder.getId().toString());
                    // 订单编号
                    profitsharingFormData.put("OrderSn",erpGoodsOrder.getOrderSn());
                    // 出账人
                    profitsharingFormData.put("UserId",shop.getCreateBy());
                    // 金额
                    profitsharingFormData.put("PayAmount",totalPrice.toString());
                    // 总金额
                    profitsharingFormData.put("TotalAmount",totalPrice.toString());
                    // 数量
                    profitsharingFormData.put("Count",erpGoodsOrder.getGoodsDto().getGoodsCount());
                    // 平台商品id
                    profitsharingFormData.put("GoodsID",erpGoodsOrder.getGoodsDto().getGoodsId());
                    System.out.println("分润接口调用参数："+profitsharingFormData);
                    // 调用接口,创建分润订单
                    String profitsharingResult = InterfaceUtils.postForm("http://119.45.237.193:9096","/api/profitsharing/insertOrderBycfg",profitsharingFormData);
                    System.out.println("分润接口调用结果："+profitsharingResult);
                    Map profitsharingResultMap = JsonUtil.transferToObj(profitsharingResult,Map.class);
                    // 成功创建
                    if(profitsharingResultMap.get("message").equals("success")){
                        // 记录出入帐任务id
                        Map profitsharingData = (Map) profitsharingResultMap.get("data");
                        // 创建记录对象
                        ErpGoodsOrderAccounts erpGoodsOrderAccounts = new ErpGoodsOrderAccounts();
                        // erp订单id
                        erpGoodsOrderAccounts.setErpOrderId(erpGoodsOrder.getId());
                        // 出账id
                        erpGoodsOrderAccounts.setGetId(Long.parseLong(profitsharingData.get("get_id").toString()));
                        // 出账状态
                        erpGoodsOrderAccounts.setGetStatus(0);
                        // 入账id
                        erpGoodsOrderAccounts.setSetId(Long.parseLong(profitsharingData.get("set_id").toString()));
                        // 入账状态
                        erpGoodsOrderAccounts.setSetStatus(0);
                        // 保存数据库
                        erpGoodsOrderAccountsService.save(erpGoodsOrderAccounts);
                    }
                    // 查询是否已经存在记录
                    OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderIdAndIsDistribution(erpGoodsOrder.getId(),"0");
                    // 如果不存在记录
                    if(orderExternalGoods == null){
                        // 创建对象
                        orderExternalGoods = new OrderExternalGoods();
                        // type = 2 外部订单
                        orderExternalGoods.setType(2L);
                        // erp订单id
                        orderExternalGoods.setOrderId(erpGoodsOrder.getId());
                        // 是否分销成功
                        orderExternalGoods.setIsDistribution("0");
                        // erp商品id
                        orderExternalGoods.setGoodsId(Long.parseLong(zhishuShopGoods.getId()));
                        // 商品价格
                        orderExternalGoods.setPayPrice(Long.parseLong(new BigDecimal(zhishuShopGoods.getPrice()).add(zhishuShopGoods.getShippingCost()).toString()));
                        // 需要支付的手续费
                        orderExternalGoods.setServiceCharge(Long.parseLong(zhishuShopGoods.getServiceCharge().toString()));
                        // 创建人
                        orderExternalGoods.setCreateBy(Long.parseLong(shop.getCreateBy()));
                        // 创建时间
                        orderExternalGoods.setCreatedAt(System.currentTimeMillis());
                        // 仓库的创建人
                        orderExternalGoods.setDeptUseId(zhishuShopGoods.getDepotUserId());
                        // 保存数据库
                        orderExternalGoodsService.insert(orderExternalGoods);
                        // 关联订单与匹配成功的商品，并且扣减库存
                        int inventory = Integer.parseInt(zhishuShopGoods.getInventory().toString()) - quantity;
                        // 执行库存同步
                        synchronizeStock(zhishuShopGoods.getId(),inventory,Integer.parseInt(zhishuShopGoods.getInventory().toString()),shop.getCreateBy(),"2",erpGoodsOrder.getId());
                        // 记录日志
                        log += "使用匹配的商品，已执行商品库存扣减并执行库存同步。" +
                                "扣除费用详情："+
//                                "仓库卖价："+new BigDecimal(zhishuShopGoods.getPrice()).divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "元" +
//                                "运费："+zhishuShopGoods.getShippingCost().divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "元" +
//                                "手续费："+zhishuShopGoods.getServiceCharge().divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "元" +
                                "共计金额：" +totalPrice.add(zhishuShopGoods.getServiceCharge()).divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "元";

                    }
                    // 修改下发状态：已下发
                    erpGoodsOrder.setIsIssue(1L);
                    // 执行修改
                    update(erpGoodsOrder);
                }else{
                    // 记录日志
                    log += "使用匹配的商品，但扣款发生错误。需要进行手动下发操作。账户余额："+sysUser.getBalance().divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)+ "元";
                }
            }else {
                // 记录日志
                log += "使用匹配的商品。但账户余额不足，无法自动下发。需要充值余额然后进行手动下发操作。账户余额："+sysUser.getBalance().divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)+ "元";
            }
        }else{
            // 关联订单与匹配成功的商品，并且依旧是自己的仓库中的商品，直接扣减库存
            OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderIdAndIsDistribution(erpGoodsOrder.getId(),"0");
            // 如果下发商品表为空
            if(orderExternalGoods == null){
                // 创建对象
                orderExternalGoods = new OrderExternalGoods();
                //type = 1内部订单
                orderExternalGoods.setType(1L);
                // erp订单id
                orderExternalGoods.setOrderId(erpGoodsOrder.getId());
                // 是否分销成功
                orderExternalGoods.setIsDistribution("0");
                // erp商品id
                orderExternalGoods.setGoodsId(Long.parseLong(zhishuShopGoods.getId()));
                // 商品价格
                orderExternalGoods.setPayPrice(0L);
                // 需要支付的手续费
                orderExternalGoods.setServiceCharge(0L);
                // 创建人
                orderExternalGoods.setCreateBy(Long.parseLong(shop.getCreateBy()));
                // 创建时间
                orderExternalGoods.setCreatedAt(System.currentTimeMillis());
                // 仓库的创建人
                orderExternalGoods.setDeptUseId(Long.parseLong(shop.getCreateBy()));
                // 保存
                orderExternalGoodsService.insert(orderExternalGoods);
            }
            System.out.println("执行分销操作:内部订单操作");
            // 扣减后的库存
            int inventory = Integer.parseInt(zhishuShopGoods.getInventory().toString()) - quantity;
            // 库存同步
            synchronizeStock(zhishuShopGoods.getId(),inventory,Integer.parseInt(zhishuShopGoods.getInventory().toString()),shop.getCreateBy(),"2",erpGoodsOrder.getId());
            // 日志
            log += "使用匹配的商品，已执行商品库存扣减并执行库存同步。货号："+zhishuShopGoods.getArtNo();
            // 修改下发状态：已下发
            erpGoodsOrder.setIsIssue(1L);
            // 执行修改操作
            update(erpGoodsOrder);
        }

        return log;
    }

    /**
     * 查询符合条件的自营书品
     * @param isbn          isbn
     * @param inventory     库存
     * @param shop          店铺信息
     * @return
     */
    @Override
    public List<ZhishuShopGoods> selectZhishuShopGoods(String isbn,Long inventory,Shop shop,WarehouseSettings warehouseSettings,ErpGoodsOrder erpGoodsOrder){
        // 查询分销中且库存充足的isbn自营商品
        ZhishuShopGoods zhishuShopGoodsBo = new ZhishuShopGoods();
        // isbn
        zhishuShopGoodsBo.setIsbn(isbn);
        // 库存数量
        zhishuShopGoodsBo.setInventory(inventory);
        // 删除标记
        zhishuShopGoodsBo.setDelFlag("0");
        // 是否分销
        zhishuShopGoodsBo.setIsJoinDistribution(1);
        // 设置表不为空
        if(warehouseSettings != null){
            // 获取匹配规则
            for (UserSettingsAttribute userSettingsAttribute : warehouseSettings.getUserSettingsAttributeList()){
                // 发货地
                if(userSettingsAttribute.getAttributeId() == 4){
                    // 存储到商品对象中
                    zhishuShopGoodsBo.setPlaceOfDispatch(userSettingsAttribute.getAttributeValue());
                }else if(userSettingsAttribute.getAttributeId() == 6){
                    //品相 存储到商品对象中
                    zhishuShopGoodsBo.setConditionCode(userSettingsAttribute.getAttributeValue());
                }
            }
        }
        //满足条件的商品
        List<ZhishuShopGoods> zhishuShopGoodsVoList = new ArrayList<>();
        // 分页信息
        int pageNum = 0;
        // 每页多少条
        int pageSize = 100;
        while(zhishuShopGoodsVoList.isEmpty()){
            // 分页信息
            zhishuShopGoodsBo.setPageNum(pageNum);
            // 每页多少条
            zhishuShopGoodsBo.setPageSize(pageSize);
            // 查询匹配的商品
            List<ZhishuShopGoods> zhishuShopGoodsList = zhishuShopGoodsService.selectList(zhishuShopGoodsBo);
            // TODO 修改仓库运费信息 快照形式 不要循环
            // 循环获取到的商品
            for (ZhishuShopGoods zhishuShopGoodsVo : zhishuShopGoodsList){
                // 获取查询到的运费以及手续费
                if(Long.parseLong(shop.getCreateBy()) != zhishuShopGoodsVo.getUserId()) {
                    // 格式化省份字段内容
                    String province = erpGoodsOrder.getProvince().replace("省","").replace("市","").replace("自治区","");
                    // 获取运费
                    BigDecimal shippingCost = getShippingCost(zhishuShopGoodsVo.getDepotId(), province,Integer.parseInt(inventory+""),shop);
                    // 获取手续费百分比
                    int percentageInt = getPercentageInt(shop.getShopType());
                    // 手续费转为小数
                    BigDecimal percentage = new BigDecimal(percentageInt).divide(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
                    //获取手续费 （仓库卖价 + 运费） * 百分比 不保留小数
                    BigDecimal serviceCharge = new BigDecimal(zhishuShopGoodsVo.getPrice())
                            .add(shippingCost)
                            .multiply(percentage)
                            .setScale(0, RoundingMode.HALF_UP);
                    // 运费
                    zhishuShopGoodsVo.setShippingCost(shippingCost);
                    // 手续费
                    zhishuShopGoodsVo.setServiceCharge(serviceCharge);
                    // 总费用
                    zhishuShopGoodsVo.setTotalCost( new BigDecimal(zhishuShopGoodsVo.getPrice()).add(shippingCost));
                }else{
                    //如果一样则不需要运费和手续费 运费
                    zhishuShopGoodsVo.setShippingCost(new BigDecimal("0"));
                    // 手续费
                    zhishuShopGoodsVo.setServiceCharge(new BigDecimal("0"));
                    // 总费用
                    zhishuShopGoodsVo.setTotalCost( new BigDecimal(zhishuShopGoodsVo.getPrice()));
                }
                // 设置为空的话，直接符合条件
                if(warehouseSettings == null){
                    // 追加到符合条件商品表里
                    zhishuShopGoodsVoList.add(zhishuShopGoodsVo);
                }else if(new BigDecimal(erpGoodsOrder.getOrderTotal()).subtract(zhishuShopGoodsVo.getTotalCost()).compareTo(warehouseSettings.getProfitFloor()) >= 0){
                    // 计算订单价与仓库价的利润是否符合匹配规则 符合则追加到符合条件商品表里
                    zhishuShopGoodsVoList.add(zhishuShopGoodsVo);
                }
            }
            // 符合条件商品表为空
            if (zhishuShopGoodsList.isEmpty()){
                // 返回
                break;
            }
            // 页数+1
            pageNum++;
        }
        // 将符合条件商品表 按价格从低到高排序
        List<ZhishuShopGoods> sortedList = zhishuShopGoodsVoList.stream()
                .sorted(Comparator.comparing(ZhishuShopGoods::getTotalCost))
                .collect(Collectors.toList());
        // 返回排序好的商品列表
        return sortedList;
    }



    /**
     * 库存同步以及订单下发
     * @param shopGoodsId
     * @param inventory
     * @param oldInventory
     * @param createBy
     * @param type
     */
    @Override
    public String synchronizeStock(String shopGoodsId,int inventory,int oldInventory,String createBy,String type,Long erpOrderId){
        // 创建商品对象
        ZhishuShopGoods zhishuShopGoods = new ZhishuShopGoods();
        // id
        zhishuShopGoods.setId(shopGoodsId);
        // 新库存
        zhishuShopGoods.setInventory(Long.parseLong(inventory+""));
        // 旧库存
        zhishuShopGoods.setOldInventory(Long.parseLong(oldInventory+""));
        // 操作人
        zhishuShopGoods.setUserId(Long.parseLong(createBy));
        // 更新自营商品表
        int mark = zhishuShopGoodsService.updateInventory(zhishuShopGoods, type,erpOrderId);
        System.out.println("扣减自营商品库存操作已完成");
        String log = "库存同步操作记录：";
        if (mark > 0){
            // 根据erp商品id查询已发布记录
            List<ShopGoodsPublished> shopGoodsPublishedList = shopGoodsPublishedService.selectByShopGoodsId(shopGoodsId);
            // 如果已发布记录不存在
            if(shopGoodsPublishedList.isEmpty()){
                // 记录日志
                log += "无已发布记录";
            }else{
                // 循环已发布商品记录，将所有已发布商品进行库存同步
                for (ShopGoodsPublished sgp : shopGoodsPublishedList){
                    // 创建迪纳普对象
                    Shop editShop = new Shop();
                    // 店铺id
                    editShop.setId(Long.parseLong(sgp.getShopId()));
                    // 店铺类型
                    editShop.setShopType(sgp.getShopType());
                    // 平台店铺id
                    editShop.setMallId(Long.parseLong(sgp.getMallId()));
                    // token
                    editShop.setToken(sgp.getToken());
                    if(sgp.getShopType().equals("1")){
                        // 调用拼多多修改库存
                        editStockService.pddEditStock(editShop,sgp.getPlatformId(),inventory+"");
                        log += "拼多多店铺："+sgp.getShopName() +":成功;";
                    } else if (sgp.getShopType().equals("2")){
                        // 调用孔夫子修改库存
                        editStockService.kfzEditStock(sgp.getToken(),sgp.getPlatformId(),inventory+"");
                        log += "孔夫子店铺："+sgp.getShopName() +":成功;";
                    } else if (sgp.getShopType().equals("5")){
                        editShop.setShopKey(sgp.getShopKey());
                        // 调用闲鱼修改库存
                        editStockService.xyEditStock(editShop,sgp.getPlatformId(),inventory+"");
                        log += "闲鱼店铺："+sgp.getShopName() +":成功;";
                    }
                }
                // 修改订单下发状态为已下发
                if(type.equals("2")){
                    // 创建erp订单对象
                    ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
                    // 订单id
                    erpGoodsOrder.setId(erpOrderId);
                    // 下发状态
                    erpGoodsOrder.setIsIssue(1L);
                    // 修改时间
                    erpGoodsOrder.setUpdatedAt(System.currentTimeMillis());
                    // 调用修改
                    update(erpGoodsOrder);
                }
            }
        }
        // 返回日志信息
        return log;
    }

    /**
     * 获取外部订单指定仓库的运费
     * @param deptId
     * @param province
     * @param quantity
     * @param shop
     * @return
     */
    public BigDecimal getShippingCost(Long deptId,String province,int quantity,Shop shop){
        /**
         * 生成需要支付的金额
         * 仓库卖家+仓库运费+手续费
         */
        //获取仓库运费模板
        Logistics logistics = logisticsService.selectLogisticeByDeptId(deptId);
        Map shippingRange = JsonUtil.transferToObj(logistics.getShippingRange(),Map.class);
        //计价方式 0 按重量  1 按标准本书（图书专用） 2 按件数  3 单独设置运费
        String pricingMethod = logistics.getPricingMethod();
        //运费
        BigDecimal shippingCost = BigDecimal.ZERO;
        //遍历shippingRange中的所有key
        for(Object key : shippingRange.keySet()){
            if(key.toString().contains(province)){
                //获取省份的配置
                List shippingCostList = (List) shippingRange.get(key);
                if (!pricingMethod.equals("3")){
                    //获取首件本书、首重
                    BigDecimal headNum = new BigDecimal(shippingCostList.get(0).toString());
                    //获取首费
                    BigDecimal headCost = new BigDecimal(shippingCostList.get(1).toString());
                    //获取续件本书、续重
                    BigDecimal continueNum = new BigDecimal(shippingCostList.get(2).toString());
                    //获取续费
                    BigDecimal continueCost = new BigDecimal(shippingCostList.get(3).toString());
                    if (pricingMethod.equals("0") && shop.getShopType().equals("2")){
                        /**
                         * 按重量计算运费
                         * 目前只支持孔夫子
                         */

                    }else if (pricingMethod.equals("1") || pricingMethod.equals("2") || !shop.getShopType().equals("2")){
                        /**
                         * 按标准本书或者按件数计算运费
                         */
                        //判断headNum大于quantity
                        if (headNum.compareTo(new BigDecimal(quantity)) >= 0){
                            shippingCost = headCost;
                        }else{
                            /**
                             * 首件数小于订单购买数量减法
                             */
                            //获取续件数
                            BigDecimal continueBookNum = new BigDecimal(quantity).subtract(headNum);
                            //（首件费 +  续件数 * 续费）* 100 不保留小数
                            shippingCost = (headCost.add(continueBookNum.multiply(continueCost))).divide(new BigDecimal(100),0, RoundingMode.DOWN);
                        }
                    }
                }else if (pricingMethod.equals("3")){
                    /**
                     * 单独设置运费
                     * 直接获取运费
                     */
                    shippingCost = new BigDecimal(shippingCostList.get(0).toString()).divide(new BigDecimal(100));
                }
               break;
            }
        }
        return shippingCost.multiply(new BigDecimal(100));
    }

    @Override
    public ErpGoodsOrder selectByOrderNo(String orderNo){
        return baseMapper.selectByOrderNo(orderNo);
    }

    /**
     * 拼多多根据订单号获取订单列表
     * @param orderNo
     * @return
     */
    @Override
    public List<ErpGoodsOrder> selectListByOrderNo(String orderNo){
        return baseMapper.selectListByOrderNo(orderNo);
    }

    @Override
    public ErpGoodsOrder selectBoOrderNoAndGoodsId(String orderSn,String goodsId){
        return baseMapper.selectBoOrderNoAndGoodsId(orderSn,goodsId);
    }

    /**
     * 分页查询ERP订单（支持动态条件）
     *
     * @param order 查询条件对象
     * @return 订单列表
     */
    @Override
    public List<ErpGoodsOrder> selectOrderList(ErpGoodsOrder order){
        return baseMapper.selectOrderList(order);
    }

    /**
     * 分页查询ERP订单（支持动态条件）
     *
     * @param order 查询条件对象
     * @return 订单列表
     */
    @Override
    public List<ErpGoodsOrder> selectPageList(ErpGoodsOrder order){
        order.setPageNum((order.getPageNum()-1)*order.getPageSize());
        return baseMapper.selectPageList(order);
    }

    /**
     * 获取ERP订单总数（支持动态条件）
     *
     * @param order 查询条件对象
     * @return 订单总数
     */
    public int selectPageCount(ErpGoodsOrder order){
        return baseMapper.selectPageCount(order);
    }

    @Override
    public int insert(ErpGoodsOrder erpGoodsOrder) {
        return baseMapper.insert(erpGoodsOrder);
    }

    @Override
    public int update(ErpGoodsOrder erpGoodsOrder) {
        return baseMapper.update(erpGoodsOrder);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int fakeDeleteById(Long id) {
        return baseMapper.fakeDeleteById(id);
    }

    @Override
    public Integer countById(String id) {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();
        return baseMapper.countOrder(id, startOfDay, endOfDay);
    }

    @Override
    public Integer countAll() {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();

        return baseMapper.countOrderAll(startOfDay, endOfDay);
    }

    @Override
    public BigDecimal todaySale(String id) {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();
        return baseMapper.todaySale(id, startOfDay, endOfDay);
    }

    @Override
    public BigDecimal todaySaleAll() {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();
        return baseMapper.todaySaleAll(startOfDay, endOfDay);
    }

    @Override
    public BigDecimal monthSale(String id) {

        // 获取本月的开始时间（当月1日 00:00:00）和结束时间（当月最后一天 23:59:59）
        long startOfMonth = getStartOfMonthTimestamp();
        long endOfMonth = getEndOfMonthTimestamp();
        return baseMapper.monthSale(id, startOfMonth, endOfMonth);
    }

    @Override
    public Integer monthOrderById(String id) {
        // 获取本月的开始时间（当月1日 00:00:00）和结束时间（当月最后一天 23:59:59）
        long startOfMonth = getStartOfMonthTimestamp();
        long endOfMonth = getEndOfMonthTimestamp();
        return baseMapper.monthOrder(id, startOfMonth, endOfMonth);
    }

    @Override
    public Integer monthOrderAll() {
        // 获取本月的开始时间（当月1日 00:00:00）和结束时间（当月最后一天 23:59:59）
        long startOfMonth = getStartOfMonthTimestamp();
        System.out.println(startOfMonth);
        long endOfMonth = getEndOfMonthTimestamp();

        return baseMapper.monthOrderAll(startOfMonth, endOfMonth);
    }

    @Override
    public BigDecimal monthSaleAll() {
        long startOfMonth = getStartOfMonthTimestamp();
        long endOfMonth = getEndOfMonthTimestamp();
        return baseMapper.monthSaleAll(startOfMonth, endOfMonth);
    }

    @Override
    public Map<String, Integer> orderDisplay(String id) {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();

        // 现在返回的是 List<Map<String, Object>>
        List<Map<String, Object>> list = baseMapper.orderDisplay(id, startOfDay, endOfDay);

        // 转换为需要的 Map<String, Integer> 格式
        Map<String, Integer> finalResult = new LinkedHashMap<>();

        for (Map<String, Object> item : list) {
            // 获取省份
            String province = (String) item.get("province");

            // 获取数量（MySQL的COUNT返回的是Long）
            Object countObj = item.get("count");
            Integer count;

            if (countObj instanceof Number) {
                count = ((Number) countObj).intValue();
            } else {
                // 如果是字符串等其他类型，转换一下
                count = Integer.parseInt(countObj.toString());
            }

            finalResult.put(province, count);
        }

        return finalResult;
    }

    @Override
    public Map<String, Integer> hourOrder(String id) {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();
        // 调用mapper层获取原始数据
        List<Map<String, Object>> list = baseMapper.hourOrder(id, startOfDay, endOfDay);

        // 转换为Map格式
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> map : list) {
            String hourTime = (String) map.get("hour_time");
            Long orderCount = (Long) map.get("order_count");
            result.put(hourTime, orderCount.intValue());
        }

        return result;
    }

    @Override
    public Map<String, Integer> orderDisplayAll() {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();

        // 现在返回的是 List<Map<String, Object>>
        List<Map<String, Object>> list = baseMapper.orderDisplayAll(startOfDay, endOfDay);

        // 转换为需要的 Map<String, Integer> 格式
        Map<String, Integer> finalResult = new LinkedHashMap<>();

        for (Map<String, Object> item : list) {
            // 获取省份
            String province = (String) item.get("province");

            // 获取数量（MySQL的COUNT返回的是Long）
            Object countObj = item.get("count");
            Integer count;

            if (countObj instanceof Number) {
                count = ((Number) countObj).intValue();
            } else {
                // 如果是字符串等其他类型，转换一下
                count = Integer.parseInt(countObj.toString());
            }

            finalResult.put(province, count);
        }

        return finalResult;
    }

    @Override
    public Map<String, Integer> hourOrderAll() {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();
        // 调用mapper层获取原始数据
        List<Map<String, Object>> list = baseMapper.hourOrderAll(startOfDay, endOfDay);

        // 转换为Map格式
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> map : list) {
            String hourTime = (String) map.get("hour_time");
            Long orderCount = (Long) map.get("order_count");
            result.put(hourTime, orderCount.intValue());
        }

        return result;
    }

    @Override
    public Map<String, Integer> orderAmountDistributionAll() {
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();
        // 取List的第一个元素
        return baseMapper.orderAmountDistributionAll(startOfDay, endOfDay).get(0);
    }


    @Override
    public Map<String, Integer> orderAmountDistribution(String id) {
        // 获取当天的开始时间（00:00:00）和结束时间（23:59:59）
        long startOfDay = getStartOfDayTimestamp();
        long endOfDay = getEndOfDayTimestamp();
        // 调用mapper层获取原始数据
        return baseMapper.orderAmountDistribution(id, startOfDay, endOfDay).get(0);
    }

    /**
     * 获取当天开始时间的时间戳（毫秒级）
     */
    private long getStartOfDayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取当天结束时间的时间戳（毫秒级）
     */
    private long getEndOfDayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取本月开始的毫秒级时间戳（当月1日 00:00:00.000）
     */
    private long getStartOfMonthTimestamp() {
        LocalDateTime startOfMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(); // 00:00:00.000
        return startOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 获取本月结束的毫秒级时间戳（当月最后一天 23:59:59.999）
     */
    private long getEndOfMonthTimestamp() {
        LocalDateTime endOfMonth = LocalDate.now()
                .withDayOfMonth(LocalDate.now().lengthOfMonth())
                .atTime(LocalTime.MAX); // 23:59:59.999999999，转换为毫秒会变成23:59:59.999
        return endOfMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}