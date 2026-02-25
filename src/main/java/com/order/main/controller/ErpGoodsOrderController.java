package com.order.main.controller;


import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dto.XyOrderDto;
import com.order.main.entity.*;
import com.order.main.service.*;
import com.order.main.service.impl.RedisService;
import com.order.main.util.*;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import com.pdd.pop.sdk.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.sql.SQLOutput;
import java.util.*;

/**
 * 平台订单
 *
 * @author yxy
 * @date 2025-12-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/erpGoodsOrder")
public class ErpGoodsOrderController  {

    private final IErpGoodsOrderService erpGoodsOrderService;
    private final IShopService shopService;
    private final IProfitconfigService profitconfigService;
    private final IZhishuShopGoodsService zhishuShopGoodsService;
    private final IWarehouseSettingsService warehouseSettingsService;
    private final IUserSettingsAttributeService userSettingsAttributeService;
    private final IShopGoodsPublishedService shopGoodsPublishedService;
    private final IOrderExternalGoodsService orderExternalGoodsService;
    private final IStockChangeLogService stockChangeLogService;
    private final IRunningTaskService runningTaskService;

    @Autowired
    private TokenUtils tokenUtils;

    /**
     * 根据id获取订单信息
     * @return
     */
    @GetMapping("/getErpGoodsOrderById")
    public ErpGoodsOrder getErpGoodsOrderById(Long id) {
        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(id);
        return erpGoodsOrder;
    }

    /**
     * 查询平台订单列表
     * @param erpGoodsOrder
     * @return
     */
    @GetMapping("/getListPage")
    public Map getListPage(ErpGoodsOrder erpGoodsOrder){
        // 定义Map
        Map map = new HashMap();
        // 查询列表
        map.put("data",erpGoodsOrderService.selectPageList(erpGoodsOrder));
        // 查询总数
        map.put("total",erpGoodsOrderService.selectPageCount(erpGoodsOrder));
        // 返回
        return map;
    }

    /**
     * 咸鱼消息推送
     * @param xyOrderDto
     */
    @PostMapping("/xyOrder")
    public void xyOrder(@RequestBody XyOrderDto xyOrderDto){
        System.out.println("订单入参："+JsonUtil.transferToJson(xyOrderDto));
        // 获取店铺信息
        Shop shop = shopService.selectShopByShopKey(xyOrderDto.getUserName());
        // 判断 店铺信息 是否存在
        if(shop != null){
            // 获取订单号
            String orderNo = xyOrderDto.getOrderNo();
            // 订单具体实现方法
            int mark = erpGoodsOrderService.xyOrderPush(shop,orderNo,false);
            System.out.println(mark);
        }else{
            System.out.println("订单号："+xyOrderDto.getOrderNo()+";会员名："+xyOrderDto.getUserName());
        }
    }

    /**
     * 获取商品信息
     * @param orderId          订单id
     * @return
     */
    @GetMapping("/getZhishuShopGoods")
    public List<ZhishuShopGoods> getZhishuShopGoods(Long orderId){
        // 获取订单信息
        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(orderId);
        // 获取店铺信息
        Shop shop = shopService.queryById(erpGoodsOrder.getShopErpId());
        // 库存
        Long inventory = -1L;
        // isbn
        String isbn = "";

        Map map =  JsonUtil.transferToObj(erpGoodsOrder.getItemList(),Map.class);
        // 下单数量
        inventory = map.get("goodsCount") == null ? 0L : Long.parseLong(map.get("goodsCount").toString());
        String artNo = "";
        try{
            // 店铺类型是闲鱼
            if(shop.getShopType().equals("5") || shop.getShopType().equals("2")){
                // 咸鱼：获取货号
                artNo = map.get("outerGoodsId").toString();
            }else if(shop.getShopType().equals("1") ){
                // 拼多多
                artNo = map.get("outerId").toString();
            }
            if (artNo.contains("9787")){
                isbn = artNo;
            }else{
                // 根据货号查询
                ZhishuShopGoods zhishuShopGoods = zhishuShopGoodsService.selectByArtNo(artNo);
                if (zhishuShopGoods == null){
                    ShopGoodsPublished shopGoodsPublished = shopGoodsPublishedService.selectByShopIdAndPlatformId(shop.getId().toString(), map.get("goodsId").toString());
                    if (shopGoodsPublished != null){
                        isbn = shopGoodsPublished.getIsbn();
                    }
                }else{
                    // isbn
                    isbn = zhishuShopGoods.getIsbn();
                }

            }
        }catch (Exception e){
            System.out.println("获取isbn失败："+e.getMessage());
            return new ArrayList<>();
        }


        // 设置对象
        WarehouseSettings warehouseSettingsBo = new WarehouseSettings();
        // 分页第几页
        warehouseSettingsBo.setPageNum(1);
        // 每页几条
        warehouseSettingsBo.setPageSize(10);
        // 删除标记
        warehouseSettingsBo.setDelFlag(0L);
        // 状态
        warehouseSettingsBo.setStatus(1L);
        // 创建人
        warehouseSettingsBo.setCreateBy(Long.parseLong(shop.getCreateBy()));
        // 获取设置（避免查询出多个启用的设置）
        List<WarehouseSettings> warehouseSettingsList = warehouseSettingsService.getPageList(warehouseSettingsBo);
        // 获取第一条仓库设置
        WarehouseSettings warehouseSettingsVo = warehouseSettingsList.isEmpty() ? null : warehouseSettingsList.get(0);
        // 仓库设置不为空
        if(warehouseSettingsVo != null){
            // 获取匹配规则
            List<UserSettingsAttribute> userSettingsAttributeList = userSettingsAttributeService.selectByWarehouseSettingsId(warehouseSettingsVo.getId());
            // 存储对象中
            warehouseSettingsVo.setUserSettingsAttributeList(userSettingsAttributeList);
        }
        // 调用匹配商品方法
        return erpGoodsOrderService.selectZhishuShopGoods(isbn,inventory,shop,warehouseSettingsVo,erpGoodsOrder);
    }

    /**
     * 订单手动下发操作
     */
    @PostMapping("/externalOrderOperation")
    public String externalOrderOperation(@RequestBody Map data){
        // 解析erp订单id
        Long orderId = Long.parseLong(data.get("orderId").toString());
        // 解析商品id
        String goodsId = data.get("goodsId").toString();
        // 获取订单信息
        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(orderId);
        // 获取店铺信息
        Shop shop = shopService.queryById(erpGoodsOrder.getShopErpId());
        // 获取商品信息
        ZhishuShopGoods zhishuShopGoods = zhishuShopGoodsService.selectById(Long.parseLong(goodsId));
        //库存
        int inventory = -1;
        if(shop.getShopType().equals("5")){
            //咸鱼：获取参数
            Map map =  JsonUtil.transferToObj(erpGoodsOrder.getItemList(),Map.class);
            //下单数量
            inventory = map.get("goodsCount") == null ? 0 : Integer.parseInt(map.get("goodsCount").toString());
        }
        // 设置对象
        WarehouseSettings warehouseSettingsBo = new WarehouseSettings();
        // 分页第几页
        warehouseSettingsBo.setPageNum(1);
        // 每页几条
        warehouseSettingsBo.setPageSize(10);
        // 删除标记
        warehouseSettingsBo.setDelFlag(0L);
        // 状态
        warehouseSettingsBo.setStatus(1L);
        // 创建人
        warehouseSettingsBo.setCreateBy(Long.parseLong(shop.getCreateBy()));
        // 获取设置（避免查询出多个启用的设置）
        List<WarehouseSettings> warehouseSettingsList = warehouseSettingsService.getPageList(warehouseSettingsBo);
        // 获取第一条仓库设置
        WarehouseSettings warehouseSettingsVo = warehouseSettingsList.isEmpty() ? null : warehouseSettingsList.get(0);
        // 仓库设置不为空
        if(warehouseSettingsVo != null){
            // 获取匹配规则
            List<UserSettingsAttribute> userSettingsAttributeList = userSettingsAttributeService.selectByWarehouseSettingsId(warehouseSettingsVo.getId());
            // 存储对象中
            warehouseSettingsVo.setUserSettingsAttributeList(userSettingsAttributeList);
        }
        // 调用手动下发商品接口
        String log = erpGoodsOrderService.externalOrderOperation(shop,erpGoodsOrder,zhishuShopGoods,inventory,"订单手动下发;",warehouseSettingsVo,false);
        // 记录日志
        Boolean bool = OrderUtils.addToOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
        if(!bool){
            //记录日志错误则创建日志
            OrderUtils.createOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,shop.getShopName(),shop.getId().toString());
        }
        return log;
    }

    @GetMapping("/getErpOrderLog")
    public List getErpOrderLog(String orderNo){
        return OrderUtils.getErpOrderLogExcel(orderNo);
    }

    /**
     * 新增订单
     * @return
     */
    @PostMapping("/addErpGoodsOrder")
    public Map addErpGoodsOrder(String erpGoodsOrderStr){
        // 解析数据
        ErpGoodsOrder erpGoodsOrder = JsonUtil.transferToObj(erpGoodsOrderStr,ErpGoodsOrder.class);
        // 获取时间戳
        Long time = System.currentTimeMillis() / 1000;
        // 创建时间
        erpGoodsOrder.setCreatedAt(time);
        // 修改时间
        erpGoodsOrder.setUpdatedAt(time);
        // 新增方法
        int mark = erpGoodsOrderService.insert(erpGoodsOrder);
        Map map = new HashMap();
        map.put("code",200);
        if(mark > 0){
            map.put("msg","新增成功");
            map.put("data",erpGoodsOrder);
        }else{
            map.put("msg","新增失败");
        }
        return map;
    }

    /**
     * 库存同步方法
     */
    @PostMapping("/synchronizeStock")
    public void synchronizeStock(Long shopGoodsId,int inventory,Long erpOrderId){
        // 查询erp订单信息
        ErpGoodsOrder erpGoodsOrder = null;
        // 参数erpGoodsOrder不为空
        if(erpOrderId != null){
            erpGoodsOrder = erpGoodsOrderService.selectById(erpOrderId);
        }
        // 查询商品信息
        ZhishuShopGoods zhishuShopGoods = zhishuShopGoodsService.selectById(shopGoodsId);
        // 定义日志对象
        String log = "";
        if (zhishuShopGoods.getInventory() == 0 && inventory <= 0){
            // 如果库存为0，则不需要进行库存同步
           log = "商品："+zhishuShopGoods.getGoodsName()+";isbn："+zhishuShopGoods.getIsbn()+";货号："+zhishuShopGoods.getArtNo()+"扣减库存为0，无需库存同步";
        }else{
            // 库存同步
           log = erpGoodsOrderService.synchronizeStock(shopGoodsId+"",inventory,zhishuShopGoods.getInventory().intValue(),zhishuShopGoods.getUserId()+"","2",erpOrderId);
        }
        if(erpGoodsOrder != null){
            // 校验是否能直接写入文件，若文件不存在，则返回false
            Boolean bool = OrderUtils.addToOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,"线下订单","10000");
            if(!bool){
                // 记录日志错误则创建日志
                OrderUtils.createOrderExcelLog(erpGoodsOrder.getOrderSn(),erpGoodsOrder.getUpdatedAt().toString(),log,"线下订单","10000");
            }
        }
    }

    /**
     * 清理缓存的订单手续费
     */
    @GetMapping("/clearCache")
    public void clearCache(){
        profitconfigService.clearCache();
    }

    /**
     * 订单回填快递单号
     * @param companyName 快递公司名称
     * @param orderNo       快递单号
     * @param erpOrderId  erp订单id
     */
    @PostMapping("/orderCompanyOrder")
    public void orderCompanyOrder(String companyName,String orderNo,String erpOrderId){
        System.out.println("订单回填,参数："+companyName+"-"+orderNo+"-"+erpOrderId);
        // 获取订单信息
        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(Long.parseLong(erpOrderId));
        // 获取店铺信息
        Shop shop = shopService.queryById(erpGoodsOrder.getShopErpId());
        // 定义返回对象
        String result = "";
        if(shop.getShopType().equals("1")){
            // 拼多多 同步订单快递单号
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("order_sn",erpGoodsOrder.getOrderSn());
            jsonObject.put("tracking_number",orderNo);
            String json = jsonObject.toString();
            result = DllInitializer.executePddOrderSynchronization(PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,shop.getToken(),companyName,json);
        } else if(shop.getShopType().equals("2")){
            // 孔夫子 同步订单快递单号
            result = DllInitializer.executeKongfzOrderSynchronization(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,shop.getToken(),
                    companyName,Integer.parseInt(erpGoodsOrder.getOrderSn()),"","",orderNo,"","");
        } else if(shop.getShopType().equals("5")){
            // 闲鱼 同步订单快递单号
            // 创建传参对象
            Map xyGoodsMap = new HashMap();
            // appId
            xyGoodsMap.put("appId",shop.getMallId());
            // appSecret
            xyGoodsMap.put("appSecret",shop.getToken());
            // user_name
            String[] userNames = new String[]{shop.getShopKey()};
            xyGoodsMap.put("user_name",userNames);
            // 订单号
            xyGoodsMap.put("order_no",erpGoodsOrder.getOrderSn());
            // 快递单号
            xyGoodsMap.put("waybill_no",orderNo);
            // 快递公司名称
            xyGoodsMap.put("express_name",companyName);
            // 调用接口
            result = DllInitializer.executeXyOrderSynchronization(JsonUtil.transferToJson(xyGoodsMap));
        }

        System.out.println("调用接口返回值："+result);
    }

    /**
     * 生成仓库订单售后
     * @return
     */
    @PostMapping("/submitAfterSales")
    public Map submitAfterSales(String id,String erpAfterSalesStatus,String erpAssReason,String erpAssRemark){
        Map<String,Object> map = new HashMap<>();

        ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
        erpGoodsOrder.setId(Long.parseLong(id));
        erpGoodsOrder.setErpAfterSalesStatus(Long.parseLong(erpAfterSalesStatus));
        if(!StringUtils.isEmpty(erpAssReason)){
            // 申请原因
            erpGoodsOrder.setErpAssReason(erpAssReason);
        }
        if (!StringUtils.isEmpty(erpAssRemark)){
            // 拒绝原因
            erpGoodsOrder.setErpAssRemark(erpAssRemark);
        }

        erpGoodsOrder.setErpAssCreateAt(System.currentTimeMillis() / 1000);
        int mark = erpGoodsOrderService.update(erpGoodsOrder);
        if(mark > 0){
            map.put("code",200);
            map.put("msg","订单售后开启成功");
        }else{
            map.put("code",500);
            map.put("msg","订单售后开启失败");
        }
        return map;
    }


    /**
     * 手动同步订单
     * @return
     */
    @PostMapping("/manualOrder")
    public Map manualOrder(String shopId,String taskId,String startUpdateTime,String endUpdateTime){
        ThreadPoolUtils.execute(() -> {

            Boolean manua = true;
            // 获取店铺信息
            Shop shop = shopService.queryById(Long.parseLong(shopId));
            if (shop.getShopType().equals("1")){
                erpGoodsOrderService.pddManualOrder(shop,taskId,startUpdateTime,endUpdateTime,manua);
            }else if (shop.getShopType().equals("2")){
                // 孔夫子
                erpGoodsOrderService.kfzManualOrder(shop,taskId,startUpdateTime,endUpdateTime,manua);
            }else if (shop.getShopType().equals("5")){
                // 闲鱼
                long startTime = DateUtils.parseDateTimeToTimestamp(startUpdateTime) / 1000;
                long endTime = DateUtils.parseDateTimeToTimestamp(endUpdateTime) / 1000;

                int pageNo = 1;
                int pageSize = 100;

                while (true){
                    // 日志记录
                    List<RunningTask> runningTaskList = new ArrayList<>();

                    JSONObject jsonObject = new JSONObject();
                    Long[] updateTimeArr = new Long[]{startTime,endTime};
                    jsonObject.put("update_time",updateTimeArr);
                    // appId
                    jsonObject.put("appId",shop.getMallId());
                    // appSecret
                    jsonObject.put("appSecret",shop.getToken());
                    jsonObject.put("page_no",pageNo);
                    jsonObject.put("page_size",pageSize);

                    String json = jsonObject.toString();

                    String result = DllInitializer.xyGetOrderList(json);

                    Map resultMap = JsonUtil.transferToObj(result,Map.class);

                    String msg = resultMap.get("msg") == null ? "" : resultMap.get("msg").toString();
                    if (msg.equals("OK")){
                        Map data = (Map) resultMap.get("data");
                        int count = Integer.parseInt(data.get("count").toString());
                        if (count > 0){
                            List list = (List) data.get("list");
                            for (int i = 0; i < list.size(); i++){
                               Map order = (Map) list.get(i);
                               Map goods = (Map) order.get("goods");
                               String orderSn = order.get("order_no").toString();
                               String itemId = goods.get("product_id").toString();
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
                                Long  orderStatus = OrderUtils.xyGetOrderStatus(Integer.parseInt(order.get("order_status").toString()));
                                if (orderStatus == 1){
                                    callBackData = "订单号："+orderSn+";订单状态：待付款;跳过";
                                }else{
                                    // 定义订单对象
                                    ErpGoodsOrder erpGoodsOrder = null;
                                    try{
                                        // id查询erp订单是否存在
                                        erpGoodsOrder = erpGoodsOrderService.selectBoOrderNoAndGoodsId(orderSn,itemId);
                                    } catch (Exception e) {
                                        callBackData = "查询异常,异常参数:订单号:"+orderSn+";商品id:"+itemId;
                                        // 打印异常
                                        e.printStackTrace();
                                    }
                                    if (erpGoodsOrder == null){
                                        erpGoodsOrderService.xyOrderPush(shop,orderSn,true);

                                        callBackData += "订单号："+orderSn+";商品id："+itemId+";重新执行订单库存同步操作;";
                                    }else{
                                        // 校验是否下发
                                        OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderId(erpGoodsOrder.getId());
                                        // 未下发则重新执行下单操作
                                        if (orderExternalGoods == null){
                                            erpGoodsOrderService.xyOrderPush(shop,orderSn,true);
                                            callBackData += "订单号："+orderSn+";商品id："+itemId+";订单未下发，重新执行下发流程;";
                                        }else{
                                            callBackData += "订单号："+orderSn+";商品id："+itemId+";订单已下发;";
                                        }
                                    }
                                }
                                runningTask.setCallBackData(callBackData);
                                runningTaskList.add(runningTask);
                            }

                            if (runningTaskList.size() > 0){
                                runningTaskService.batchInsert(runningTaskList);
                            }
                        }else{
                            break;
                        }
                    }else{
                        break;
                    }
                    pageNo++;
                }
            }
        });

        Map result = new HashMap();
        result.put("code",200);
        result.put("msg","开启手动同步任务");
        return result;
    }


    @GetMapping("/todayCount")
    public Integer countOrder(@RequestParam Long id, @RequestParam List<String> shopIdList) {
        if (id == 1) {
            return erpGoodsOrderService.countAll();
        }

        Integer count = 0;
        for (String shopId : shopIdList) {
            count += erpGoodsOrderService.countById(shopId);
        }
        return count;
    }

    @GetMapping("/todaySale")
    public BigDecimal todaySale(@RequestParam Long id, @RequestParam List<String> shopIdList) {
        if (id == 1) {
            return erpGoodsOrderService.todaySaleAll();
        }

        BigDecimal totalSale = BigDecimal.ZERO;
        for (String shopId : shopIdList) {
            BigDecimal sale = erpGoodsOrderService.todaySale(shopId);
            if (sale != null) {
                totalSale = totalSale.add(sale);
            }
        }
        return totalSale;
    }

    @GetMapping("/monthOrder")
    public Integer monthOrder(@RequestParam Long id, @RequestParam List<String> shopIdList) {
        if (id == 1) {
            return erpGoodsOrderService.monthOrderAll();
        }
        Integer count = 0;
        for (String shopId : shopIdList) {
            count += erpGoodsOrderService.monthOrderById(shopId);
        }
        return count;
    }

    @GetMapping("/monthSale")
    public BigDecimal monthSale(@RequestParam Long id, @RequestParam List<String> shopIdList) {
        if (id == 1) {
            return erpGoodsOrderService.monthSaleAll();
        }
        BigDecimal totalSale = BigDecimal.ZERO;
        for (String shopId : shopIdList) {
            BigDecimal sale = erpGoodsOrderService.monthSale(shopId);
            if (sale != null) {
                totalSale = totalSale.add(sale);
            }
        }
        return totalSale;
    }

//    @GetMapping("/totalCount/{id}")
//    public Integer TotalOrder(@PathVariable Long id){
//        if (id == 1) {
//            return erpGoodsOrderService.monthOrderAll();
//        }
//        List<String> shopIdList = shopMapper.getShopIdByCreateBy(id);
//
//        Integer count = 0;
//
//        for (String shopId : shopIdList) {
//            count += erpGoodsOrderService.monthOrderById(shopId);
//        }
//
//        return count;
//    }
//
//    @GetMapping("/todaySale/{id}")
//    public BigDecimal todaySale(@PathVariable Long id) {
//        System.out.println("开始统计");
//        if (id == 1) {
//            return erpGoodsOrderService.todaySaleAll();
//        }
//        List<String> shopIdList = shopMapper.getShopIdByCreateBy(id);
//
//        return shopIdList.stream()
//                .map(erpGoodsOrderService::todaySale)
//                .filter(Objects::nonNull)  // 过滤掉null值
//                .reduce(BigDecimal.ZERO, BigDecimal::add);  // 累加所有销售额
//    }
//
//    @GetMapping("/totalSale/{id}")
//    public BigDecimal totalSale(@PathVariable Long id) {
//        System.out.println("开始统计");
//        if (id == 1) {
//            return erpGoodsOrderService.monthSaleAll();
//        }
//        List<String> shopIdList = shopMapper.getShopIdByCreateBy(id);
//
//        return shopIdList.stream()
//                .map(erpGoodsOrderService::monthSale)
//                .filter(Objects::nonNull)  // 过滤掉null值
//                .reduce(BigDecimal.ZERO, BigDecimal::add);  // 累加所有销售额
//    }
}
