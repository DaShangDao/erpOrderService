package com.order.main.controller;


import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.KfzSimpleDllLoader;
import com.order.main.dll.PddSimpleDllLoader;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.dto.XyOrderDto;
import com.order.main.entity.*;
import com.order.main.service.*;
import com.order.main.service.impl.RedisService;
import com.order.main.util.*;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    private final IRunningTaskService runningTaskService;
    private final ICourierLogService courierLogService;
    private final TShopGoodsPublishedService tShopGoodsPublishedService;
    private final IExpressDeliveryOrderService expressDeliveryOrderService;
    private final IOrderCompanyRetryService orderCompanyRetryService;
    private final IUserRoleService userRoleService;
    private final IPddMessageService pddMessageService;



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

    @GetMapping("/getErpGoodsOrderByOrderSn")
    @CrossOrigin(origins = "*")  // 允许所有来源访问
    public ErpGoodsOrder getErpGoodsOrderByOrderSn(String orderSn){
        return erpGoodsOrderService.selectByOrderNo(orderSn);
    }

    @GetMapping("/getErpGoodsOrderByOrderSns")
    @CrossOrigin(origins = "*")  // 允许所有来源访问
    public Map<String, Object> getErpGoodsOrderByOrderSns(String orderSns) {
        System.out.println("调用查询订单是否售后"+orderSns);
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> afterSalesList = new ArrayList<>();

        if (orderSns != null && !orderSns.isEmpty()) {
            String[] snArray = orderSns.split(",");
            for (String sn : snArray) {
                sn = sn.trim();
                if (sn.isEmpty()) continue;
                List<ErpGoodsOrder> orderList = erpGoodsOrderService.selectListByOrderNo(sn);
                if (orderList == null || orderList.isEmpty()) {
                    // 查不到订单时不判售后，宁可放行
                    continue;
                }
                // 只看售后状态字段：已发货(order_status=3)也要能回填快递单号，
                // 所以不再用 order_status != 2 判售后。多条明细任一有售后即算售后。
                Long afterSalesStatus = null;
                boolean hasAfterSales = false;
                for (ErpGoodsOrder row : orderList) {
                    if (row.getAfterSalesStatus() != null && row.getAfterSalesStatus() != 0L) {
                        hasAfterSales = true;
                        afterSalesStatus = row.getAfterSalesStatus();
                        break;
                    }
                }
                if (hasAfterSales) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("orderSn", sn);
                    item.put("afterSalesStatus", afterSalesStatus);
                    afterSalesList.add(item);
                }
            }
        }

        result.put("afterSalesCount", afterSalesList.size());
        result.put("afterSalesList", afterSalesList);
        return result;
    }

    /**
     * 批量查询订单在平台的发货状态（只读，供销售单生成出库单前提示用）
     * 与售后判定接口分离：本接口不改任何判定逻辑，仅把 orderStatus 原样返回
     *
     * @param orderSns 逗号分隔的第三方订单编号
     * @return list: 命中订单的状态；missing: 平台未查到的订单号
     */
    @GetMapping("/getPlatformStatusByOrderSns")
    public Map<String, Object> getPlatformStatusByOrderSns(String orderSns) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        if (orderSns != null && !orderSns.isEmpty()) {
            String[] snArray = orderSns.split(",");
            for (String sn : snArray) {
                String orderSn = sn == null ? "" : sn.trim();
                if (orderSn.isEmpty()) continue;
                List<ErpGoodsOrder> orderList = erpGoodsOrderService.selectListByOrderNo(orderSn);
                if (orderList == null || orderList.isEmpty()) {
                    missing.add(orderSn);
                    continue;
                }
                // 同一平台单可能有多条明细行，状态一致，取首行即可
                ErpGoodsOrder row = orderList.get(0);
                Map<String, Object> item = new HashMap<>();
                item.put("orderSn", orderSn);
                item.put("orderStatus", row.getOrderStatus());
                item.put("afterSalesStatus", row.getAfterSalesStatus());
                item.put("trackingNumber", row.getTrackingNumber());
                list.add(item);
            }
        }
        result.put("list", list);
        result.put("missing", missing);
        return result;
    }

    @GetMapping("/getList")
    public  List<ErpGoodsOrder> getList(ErpGoodsOrder erpGoodsOrder){
        return erpGoodsOrderService.selectOrderList(erpGoodsOrder);
    }

    @GetMapping("/getListByIds")
    @CrossOrigin(origins = "*")  // 允许所有来源访问
    public List<ErpGoodsOrder> getListByIds(String ids){
        List<Long> idsList = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            String[] idArray = ids.split(",");
            for (String id : idArray) {
                idsList.add(Long.parseLong(id.trim()));
            }
        }
        return erpGoodsOrderService.selectByIds(idsList);
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
                List<ZhishuShopGoods> zhishuShopGoodsList = zhishuShopGoodsService.selectByArtNo(artNo);
                if (zhishuShopGoodsList.isEmpty()){
                    ShopGoodsPublished shopGoodsPublished = shopGoodsPublishedService.selectByShopIdAndPlatformId(shop.getId().toString(), map.get("goodsId").toString());
                    if (shopGoodsPublished != null){
                        isbn = shopGoodsPublished.getIsbn();
                    }
                }else{
                    // isbn
                    isbn = zhishuShopGoodsList.get(0).getIsbn();
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
        Map map =  JsonUtil.transferToObj(erpGoodsOrder.getItemList(),Map.class);
        //下单数量
        int inventory = map.get("goodsCount") == null ? 0 : Integer.parseInt(map.get("goodsCount").toString());
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
            if (erpGoodsOrder == null){
                erpGoodsOrder = new ErpGoodsOrder();
                erpGoodsOrder.setId(erpOrderId);
            }
            // 库存同步
           log = erpGoodsOrderService.synchronizeStock(shopGoodsId+"",inventory,zhishuShopGoods.getInventory().intValue(),zhishuShopGoods.getUserId()+"","2",erpGoodsOrder);
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
     * @param orderNo       快递单号
     * @param erpOrderId  erp订单id
     */
    /** 孔网回填重试次数（含首次）与退避间隔（毫秒） */
    private static final int KFZ_RETRY_TIMES = 3;
    private static final long[] KFZ_RETRY_BACKOFF = new long[]{1000L, 2000L, 4000L};

    /**
     * 孔网返回是否表示回填成功。
     * 新增发货走 kongfz.order.deliver（remark=订单发货成功），改单号走 kongfz.order.redeliver
     * （remark=修改订单发货单号成功）；早期只判断「订单发货成功」，导致改单号成功也被记成失败。
     */
    private boolean isKfzDeliverSuccess(String result) {
        if (StringUtils.isEmpty(result)) {
            return false;
        }
        if (result.contains("订单发货成功") || result.contains("修改订单发货单号成功")) {
            return true;
        }
        return result.contains("\"successResponse\":") && !result.contains("\"errorResponse\":{");
    }

    /**
     * 孔网返回是否属于平台侧临时故障（值得退避重试）：
     * 3003 Service error / Order Deliver Error / curl error 7 / Connection refused / 超时。
     */
    private boolean isKfzTransientError(String result) {
        if (result == null || result.trim().isEmpty()) {
            return true;
        }
        String r = result.toLowerCase();
        return r.contains("connection refused") || r.contains("curl error")
                || r.contains("\"code\":3003") || r.contains("\"code\":2007")
                || r.contains("order deliver error") || r.contains("service error")
                || r.contains("service unavailable") || r.contains("timed out")
                || r.contains("timeout") || r.contains("网络超时");
    }


    @PostMapping("/orderCompanyOrder")
    public Map<String,Object> orderCompanyOrder(String sysDictDataVoListStr,String orderNo,String erpOrderId,String code){
        Map<String,Object> resp = new HashMap<>();
        // 兼容全角逗号：多包裹单号分隔符统一按英文逗号处理
        if (orderNo != null) {
            orderNo = orderNo.replace('，', ',');
        }
        String originOrderNo = orderNo;

        System.out.println("接口入参：orderNo:"+orderNo+";;erpOrderId:"+erpOrderId+";;code:"+code);

        ErpGoodsOrder erpGoodsOrder = null;
        Shop shop = null;
        String companyName = "";
        String result = "";
        try {
            // 获取订单信息
            erpGoodsOrder = erpGoodsOrderService.selectById(Long.parseLong(erpOrderId));
            if (erpGoodsOrder == null) {
                recordOrderCompanyResult(null, erpOrderId, "", orderNo, "", "未知店铺",
                        "订单不存在：erpOrderId=" + erpOrderId, 2);
                resp.put("code", "500");
                resp.put("msg", "订单不存在：erpOrderId=" + erpOrderId);
                return resp;
            }

            if((erpGoodsOrder.getOrderStatus() == 2L || erpGoodsOrder.getOrderStatus() == 3L)
                    && erpGoodsOrder.getAfterSalesStatus() == 0L){
                if(StringUtils.isEmpty(code)){
                    ExpressDeliveryOrder expressDeliveryOrder = expressDeliveryOrderService.getByErpOrderId(erpOrderId);
                    if (expressDeliveryOrder != null){
                        code = expressDeliveryOrder.getType();
                    }else{
                        List<CourierLog> courierLogList =  courierLogService.getListByErpOrderId(Long.parseLong(erpOrderId));
                        code = courierLogList.get(0).getMailType();
                    }

                }else{
                    code = code.equals("yunda") ? "YUNDA" : code;
                }
                List sysDictDataVoList = JsonUtil.transferToObj(sysDictDataVoListStr,List.class);
                for (Object sysDictDataVo : sysDictDataVoList){
                    Map sysDictDataVoMap = (Map) sysDictDataVo;
                    if(code.equals(sysDictDataVoMap.get("dictValue"))){
                        companyName = sysDictDataVoMap.get("dictLabel").toString();
                        break;
                    }
                }

                // 获取店铺信息
                shop = shopService.queryById(erpGoodsOrder.getShopErpId());
                if (shop == null) {
                    recordOrderCompanyResult(null, erpOrderId, erpGoodsOrder.getOrderSn(), originOrderNo, companyName,
                            "未知店铺", "店铺不存在：shopErpId=" + erpGoodsOrder.getShopErpId(), 2);
                    resp.put("code", "500");
                    resp.put("msg", "店铺不存在：shopErpId=" + erpGoodsOrder.getShopErpId());
                    return resp;
                }
                if(shop.getShopType().equals("1")){
                    // 拼多多 同步订单快递单号
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("order_sn",erpGoodsOrder.getOrderSn());
                    jsonObject.put("tracking_number",orderNo);
                    if (StringUtils.isNotEmpty(erpGoodsOrder.getTrackingNumber()) && erpGoodsOrder.getOrderStatus() != 2L){
                        jsonObject.put("redelivery_type","2");
                    }
                    String json = jsonObject.toString();
                    result = DllInitializer.executePddOrderSynchronization(PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,shop.getToken(),companyName,json);
                } else if(shop.getShopType().equals("2")){
                    if (companyName.equals("韵达快递")){
                        companyName = "韵达速递";
                    }else if (companyName.contains("邮政")){
                        companyName = "中国邮政";
                    }else if (companyName.contains("圆通快递")){
                        companyName = "圆通速递";
                    }

                    // 补发快递
                    String moreShipmentNum = "";
                    int index = orderNo.indexOf(',');
                    if (index != -1) {
                        String first = orderNo.substring(0, index);
                        moreShipmentNum = orderNo.substring(index + 1);
                        orderNo = first;
                    }


                    // 孔网回填快递单号：平台偶发 3003 Service error / Order Deliver Error
                    // （curl error 7 Failed connect to pc-gateway.kongfz.com:80 Connection refused），
                    // 这类抖动重试往往就过了，所以退避重试 3 次（1s/2s/4s），仍失败才落库待人工处理。
                    boolean kfzOk = false;
                    String lastResult = "";
                    for (int attempt = 1; attempt <= KFZ_RETRY_TIMES; attempt++) {
                        if (StringUtils.isNotEmpty(erpGoodsOrder.getTrackingNumber()) && erpGoodsOrder.getOrderStatus() != 2L) {
                            lastResult = DllInitializer.executeKongfzOrderRedeliver(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,shop.getToken(),
                                    companyName,Long.parseLong(erpGoodsOrder.getOrderSn()),"","",orderNo,"",moreShipmentNum);
                        }else{
                            // 孔夫子 同步订单快递单号
                            lastResult = DllInitializer.executeKongfzOrderSynchronization(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,shop.getToken(),
                                    companyName,Long.parseLong(erpGoodsOrder.getOrderSn()),"","",orderNo,"",moreShipmentNum);
                        }
                        result = lastResult;
                        if (isKfzDeliverSuccess(lastResult)) {
                            kfzOk = true;
                            System.out.println("[孔网回填] 成功 orderSn=" + erpGoodsOrder.getOrderSn() + " 尝试次数=" + attempt);
                            break;
                        }
                        if (!isKfzTransientError(lastResult) || attempt == KFZ_RETRY_TIMES) {
                            break;
                        }
                        long waitMs = KFZ_RETRY_BACKOFF[Math.min(attempt - 1, KFZ_RETRY_BACKOFF.length - 1)];
                        System.out.println("[孔网回填] 第" + attempt + "次失败（平台临时故障），" + waitMs + "ms 后重试 orderSn="
                                + erpGoodsOrder.getOrderSn() + " 返回=" + StringUtils.substring(lastResult, 0, 300));
                        try {
                            Thread.sleep(waitMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                } else if(shop.getShopType().equals("5")){

                    if (companyName.contains("圆通快递")){
                        companyName = "圆通速递";
                    }

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
                    if (StringUtils.isNotEmpty(erpGoodsOrder.getTrackingNumber()) && erpGoodsOrder.getOrderStatus() != 2L){
                        // 调用接口
                        result = DllInitializer.executeXyModifyWaybillNo(JsonUtil.transferToJson(xyGoodsMap));
                    }else{
                        // 调用接口
                        result = DllInitializer.executeXyOrderSynchronization(JsonUtil.transferToJson(xyGoodsMap));
                    }
                }
                // 无论哪个平台、成功还是失败，统一落库到 order_company_retry
                int recordStatus;
                if (isDeliverSuccess(shop.getShopType(), result)) {
                    recordStatus = 1;
                } else if (isRetryableDeliverError(result) && "1".equals(shop.getShopType())) {
                    recordStatus = 0;
                } else {
                    recordStatus = 2;
                }
                recordOrderCompanyResult(shop, erpOrderId, erpGoodsOrder.getOrderSn(), originOrderNo, companyName,
                        platformName(shop), result, recordStatus);
                System.out.println("调用接口返回值："+result);
                resp.put("code", recordStatus == 1 ? "200" : "500");
                resp.put("msg", recordStatus == 1 ? "回传成功"
                        : StringUtils.substring(String.valueOf(result), 0, 200));
                resp.put("recordStatus", recordStatus);
                return resp;
            }else{
                // 订单未处于待发货状态，同样落库记录，便于排查「PSI 已发货但平台未回填」
                shop = shopService.queryById(erpGoodsOrder.getShopErpId());
                recordOrderCompanyResult(shop, erpOrderId, erpGoodsOrder.getOrderSn(), originOrderNo, "",
                        platformName(shop), "未回填：订单状态不符（orderStatus=" + erpGoodsOrder.getOrderStatus()
                                + "，afterSalesStatus=" + erpGoodsOrder.getAfterSalesStatus() + "）", 2);
                resp.put("code", "500");
                resp.put("msg", "未回填：订单状态不符（orderStatus=" + erpGoodsOrder.getOrderStatus()
                        + "，afterSalesStatus=" + erpGoodsOrder.getAfterSalesStatus() + "）");
                return resp;
            }
        } catch (Exception e) {
            // 任何异常都要落库，避免「PSI 显示已发货、实际没回填」时无痕迹可查
            System.err.println("回填快递单号异常: " + e.getMessage());
            e.printStackTrace();
            recordOrderCompanyResult(shop, erpOrderId,
                    erpGoodsOrder == null ? "" : erpGoodsOrder.getOrderSn(), originOrderNo, companyName,
                    platformName(shop), "调用异常：" + e.getClass().getSimpleName() + "：" + e.getMessage(), 2);
            resp.put("code", "500");
            resp.put("msg", "调用异常：" + e.getClass().getSimpleName() + "：" + e.getMessage());
            return resp;
        }
    }

    /**
     * 回填快递单号结果统一落库：所有平台、成功与失败都记录到 order_company_retry
     * status: 0 待重试（仅拼多多网络超时）1 成功 2 失败/未处理
     */
    private void recordOrderCompanyResult(Shop shop, String erpOrderId, String orderSn, String orderNo,
                                          String companyName, String platform, String result, int status) {
        try {
            OrderCompanyRetry retry = new OrderCompanyRetry();
            retry.setShopId(shop == null || shop.getId() == null ? 0L : shop.getId());
            try {
                retry.setErpOrderId(Long.parseLong(erpOrderId));
            } catch (Exception ignore) {
                // erpOrderId 非法也要留痕，用 0 占位
                retry.setErpOrderId(0L);
            }
            retry.setOrderSn(orderSn == null ? "" : orderSn);
            retry.setTrackingNumber(orderNo == null ? "" : orderNo);
            retry.setCompanyName(companyName == null ? "" : companyName);
            retry.setRetryCount(0);
            retry.setStatus(status);
            String tag = status == 1 ? "成功" : (status == 0 ? "待重试" : "失败");
            String body = result == null ? "" : result;
            if (body.length() > 60000) {
                body = body.substring(0, 60000);
            }
            retry.setLastResult("[" + platform + "][" + tag + "]" + body);
            orderCompanyRetryService.save(retry);
        } catch (Exception e) {
            System.err.println("记录回填结果失败: " + e.getMessage());
        }
    }

    /**
     * 平台回填是否成功
     */
    private boolean isDeliverSuccess(String shopType, String result) {
        if (StringUtils.isEmpty(result)) {
            return false;
        }
        String r = result.trim();
        if (r.startsWith("Error:")) {
            return false;
        }
        if ("2".equals(shopType)) {
            return isKfzDeliverSuccess(result);
        }
        if (r.contains("\"successResponse\":")) {
            return true;
        }
        if (r.contains("\"errorResponse\":{")) {
            return false;
        }
        String lower = r.toLowerCase();
        return !lower.contains("error") && !r.contains("失败") && !r.contains("异常");
    }

    /**
     * 是否属于平台临时故障（拼多多网络超时交给定时器重试）
     */
    private boolean isRetryableDeliverError(String result) {
        return result != null && (result.contains("网络超时异常") || result.contains("暂时无法识别该运单号"));
    }

    /**
     * 店铺类型中文名
     */
    private String platformName(Shop shop) {
        if (shop == null) {
            return "未知店铺";
        }
        String t = shop.getShopType();
        if ("1".equals(t)) {
            return "拼多多";
        }
        if ("2".equals(t)) {
            return "孔夫子";
        }
        if ("5".equals(t)) {
            return "闲鱼";
        }
        if ("6".equals(t)) {
            return "淘宝";
        }
        return "其他(" + t + ")";
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
                            if(list.isEmpty()){
                                break;
                            }
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
                                    // 待付款：正常同步进 ERP，但不下发 PSI（由 orderOperation 内的闲鱼闸门拦截）
                                    try {
                                        erpGoodsOrderService.xyOrderPush(shop, orderSn, true);
                                    } catch (Exception e) {
                                        System.out.println("待付款订单同步ERP失败:" + orderSn);
                                    }
                                    callBackData = "订单号："+orderSn+";订单状态：待付款，仅同步ERP，不下发PSI";
                                }else{
                                    // 定义订单对象
                                    ErpGoodsOrder erpGoodsOrder = null;
                                    try{
                                        // id查询erp订单是否存在
                                        erpGoodsOrder = erpGoodsOrderService.selectByOrderNo(orderSn);
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
                            if (pageNo >= 100){
                                System.out.println("1111");
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

    /**
     * 今日订单
     * @param id
     * @param shopIdList
     * @return
     */
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

    /**
     * 今日总额
     * @param id
     * @param shopIdList
     * @return
     */
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

    /**
     * 本月订单
     * @param id
     * @param shopIdList
     * @return
     */
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

    /**
     * 本月总额
     * @param id
     * @param shopIdList
     * @return
     */
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

    @GetMapping("/orderDisplay")
    public Map<String, Integer> orderDisplay(@RequestParam Long id) {
        if (id == 1) {
            return erpGoodsOrderService.orderDisplayAll();
        }

        return erpGoodsOrderService.orderDisplay(id.toString());
    }

    @GetMapping("/hourOrder")
    public Map<String, Integer> hourOrder(@RequestParam Long id) {
        if (id == 1) {
            return erpGoodsOrderService.hourOrderAll();
        }
        return erpGoodsOrderService.hourOrder(id.toString());
    }

    @GetMapping("/orderAmountDistribution")
    public Map<String, Integer> orderAmountDistribution(@RequestParam Long id) {
        if (id == 1) {
            return erpGoodsOrderService.orderAmountDistributionAll();
        }
        return erpGoodsOrderService.orderAmountDistribution(id.toString());
    }


    /**
     * 处理自营商品操作日志数据
     */
    @GetMapping("/handleShopStockLog")
    public String handleShopStockLog(String jsonList){
        try {
             jsonList = URLDecoder.decode(jsonList, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            System.out.println("解析数据失败");
        }
        List stockChangeLogList = JSONObject.parseArray(jsonList);
        for (Object stockChangeLog : stockChangeLogList){
            Map stockChangeLogMap = (Map) stockChangeLog;
            String aboutId = stockChangeLogMap.get("aboutId") == null ? "" : stockChangeLogMap.get("aboutId").toString();
            if (StringUtils.isNotEmpty(aboutId)){
                ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(Long.parseLong(aboutId));
                stockChangeLogMap.put("remark","店铺名称："+erpGoodsOrder.getShopErpName()+";订单编号："+erpGoodsOrder.getOrderSn());
            }else{
                stockChangeLogMap.put("remark","");
            }
        }
        return JsonUtil.transferToJson(stockChangeLogList);
    }


//    /**
//     * 进销存库存同步方法
//     */
//    @PostMapping("/synchronizeStockNew")
//    public String synchronizeStockNew(String productId,String userId,String inventory,String oldInventory,String erpGoodsId){
//        return tShopGoodsPublishedService.synchronizeStockNew(productId,Long.parseLong(userId),Integer.parseInt(inventory),Integer.parseInt(oldInventory),Long.parseLong(erpGoodsId));
//    }
//


    @GetMapping("/test")
    public void test(String erpId,String queueId,String orderType){
        // 获取未下发的订单
        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(Long.parseLong(erpId));
        Shop shop = shopService.queryById(erpGoodsOrder.getShopErpId());
        Date now = new Date();
        boolean expired = (shop.getExpirationTime() != null && shop.getExpirationTime().before(now))
                || (shop.getShopExpirationTime() != null && shop.getShopExpirationTime().before(now));
        if (expired) {
            System.out.println("店铺已过期");
        }else{
            erpGoodsOrder.setOrderType(orderType);
            erpGoodsOrder.setQueueId(queueId);
            int count = userRoleService.selecUserRole(erpGoodsOrder.getCreatedBy());
            if (erpGoodsOrder != null){
                if (erpGoodsOrder.getOrderType() == null || erpGoodsOrder.getOrderType().equals("0")){
                    WarehouseSettings warehouseSettings = erpGoodsOrderService.getWarehouseSettings(erpGoodsOrder.getCreatedBy());
                    if (warehouseSettings == null){
                        // 设置默认配置
                        warehouseSettings = new WarehouseSettings();
                        // 库存同步形式  0 下单减库存   1  支付减库存
                        warehouseSettings.setStockSynchronizeType(1L);
                        // 是否自动下发  0 否  1 是
                        warehouseSettings.setAutoIssue(1L);
                        // 是否开启亏损保护 0 否 1 是
                        warehouseSettings.setLossProtection(1L);
                        // 利润下限  默认利润1块钱
                        warehouseSettings.setProfitFloor(new BigDecimal(100));
                    }
                    // 下发
//                    tShopGoodsPublishedService.createSalesOrder(erpGoodsOrder,warehouseSettings);
                }else if(erpGoodsOrder.getOrderType().equals("1")){
                    // 订单完成的操作
                    tShopGoodsPublishedService.orderFinish(erpGoodsOrder);
                }else if (erpGoodsOrder.getOrderType().equals("2")){
                    // 订单退款
                    tShopGoodsPublishedService.orderReturnh(erpGoodsOrder);
                }
            }
        }
    }


    @GetMapping("/test2")
    public void test2(String content){

        Message message = new Message();

        message.setType("ceshi");
        message.setMallID(1234567866L);
        message.setContent(content);



//        Message message = new Message();
//        message.setContent(content);
//        erpGoodsOrderService.pddReviewRejected(message);

//        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderService.selectById(Long.parseLong(erpId));
//        tShopGoodsPublishedService.synchronizeStockNew(productId,userId,0,0,erpGoodsOrder);
    }

    @GetMapping("/test3")
    public void test3(String accessToken,String itemId){
        net.minidev.json.JSONObject jsonObject = new net.minidev.json.JSONObject();
        jsonObject.put("itemId",itemId);
        jsonObject.put("simplify",1);
        String res = KfzSimpleDllLoader.executeKongfzShopItemDetail(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,accessToken,jsonObject.toString());
        Map resMap = JsonUtil.transferToObj(res,Map.class);
        System.out.println(res);
    }
}
