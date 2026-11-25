package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.KfzSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.dto.TShopGoodsPublishedDto;
import com.order.main.entity.*;
import com.order.main.mapper.ErpGoodsOrderMapper;
import com.order.main.mapper.TShopGoodsPublishedMapper;
import com.order.main.service.*;
import com.order.main.util.ClientConstantUtils;
import com.order.main.util.InterfaceUtils;
import com.order.main.util.MaskUtils;
import com.order.main.util.UrlUtil;
import com.order.main.util.DateUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import com.sun.jna.platform.mac.SystemB;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 已发布商品信息Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TShopGoodsPublishedServiceImpl implements TShopGoodsPublishedService {

    private final TShopGoodsPublishedMapper tShopGoodsPublishedMapper;
    private final IEditStockService editStockService;
    private final ISynchronizationShopLogService synchronizationShopLogService;
    private final IShopService shopService;
    private final IErpGoodsOrderQueueService erpGoodsOrderQueueService;
    private final ISysUserService userService;
    private final IPsiEmployeesService psiEmployeesService;
    private final RedisService redisService;
    private final IErpDistributionOrderService erpDistributionOrderService;
    private final IErpDistributionAfterSalesService afterSalesService;
    private final IErpDistributionOrderLogService erpDistributionOrderLogService;
    private final ErpGoodsOrderMapper erpGoodsOrderMapper;

    /** 旧订单推送拦截：下单时间超过该天数不再下发 PSI */
    private static final long PUSH_MAX_AGE_DAYS = 15L;


    @Override
    @DS("taskDb")
    public TShopGoodsPublishedDto selectById(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return tShopGoodsPublishedMapper.selectById(id);
        } catch (Exception e) {
            log.error("根据ID查询已发布商品失败, id: {}", id, e);
            return null;
        }
    }

    /**
     * 根据平台商品id查询
     * @param trilateralId
     * @return
     */
    @Override
    @DS("taskDb")
    public List<TShopGoodsPublishedDto> selectByTrilateralId(Long trilateralId){
        return tShopGoodsPublishedMapper.selectByTrilateralId(trilateralId);
    }

    /**
     * 根据进销存商品id查询
     * @param productId
     * @return
     */
    @Override
    @DS("taskDb")
    public List<TShopGoodsPublishedDto> selectByProductId(Long productId,Long userId,String isDistribution){
        return tShopGoodsPublishedMapper.selectByProductId(productId,userId,isDistribution);
    }


    /**
     * 查询一条已被删除的数据
     * @param productId
     * @param userId
     * @param trilateralId
     * @return
     */
    @Override
    @DS("taskDb")
    public TShopGoodsPublishedDto selectDelFlag(Long productId,Long userId,Long trilateralId){
        return tShopGoodsPublishedMapper.selectDelFlag(productId,userId,trilateralId);
    }


    @Override
    @DS("taskDb")
    public int update(Long id){
        return tShopGoodsPublishedMapper.updateShopGoodsPublished(id);
    }

    @Override
    @DS("taskDb")
    public int updateShopGoodsPublishedRecover(Long id){
        return tShopGoodsPublishedMapper.updateShopGoodsPublishedRecover(id);
    }

    @Override
    @DS("taskDb")
    public int deleteById(Long id){
        return tShopGoodsPublishedMapper.deleteById(id);
    }

    @Override
    @DS("taskDb")
    public int deleteByErpShopId(Long erpShopId){
        return tShopGoodsPublishedMapper.deleteByErpShopId(erpShopId);
    }

    @Override
    @DS("taskDb")
    public int deleteByTrilateralId(Long trilateralId){
        return tShopGoodsPublishedMapper.deleteByTrilateralId(trilateralId);
    }

    @Override
    @DS("taskDb")
    public void createSalesOrder(ErpGoodsOrder erpGoodsOrder,WarehouseSettings warehouseSettings) {
        try{
            System.out.println("【开始执行推送销售订单操作】-----------------------："+JsonUtil.transferToJson(erpGoodsOrder));

            // ===== 旧订单推送拦截：下单时间超过 15 天不再下发 PSI =====
            Long orderCreatedAt = erpGoodsOrder.getCreatedAt();
            if (orderCreatedAt != null && orderCreatedAt > 0) {
                // created_at 库里 10位(秒)/13位(毫秒) 混存，先统一成毫秒
                long createdAtMs = String.valueOf(orderCreatedAt).length() == 13 ? orderCreatedAt : orderCreatedAt * 1000;
                long ageMs = System.currentTimeMillis() - createdAtMs;
                if (ageMs > PUSH_MAX_AGE_DAYS * 24L * 3600 * 1000) {
                    long days = ageMs / (24L * 3600 * 1000);
                    String msg = "旧订单推送：" + days + "天订单（下单时间 "
                            + DateUtils.formatTimestampToGMT8(createdAtMs) + "），不再推送";
                    try {
                        String queueIdStr = erpGoodsOrder.getQueueId();
                        if (queueIdStr != null && !queueIdStr.trim().isEmpty()
                                && !"0".equals(queueIdStr.trim())) {
                            ErpGoodsOrderQueue queue = new ErpGoodsOrderQueue();
                            queue.setId(Long.parseLong(queueIdStr.trim()));
                            queue.setStatus("4");   // 4=跳过，前端显示「跳过」且隐藏手动下发按钮
                            queue.setMsg(msg);
                            erpGoodsOrderQueueService.update(queue);
                        }
                    } catch (Exception e) {
                        System.out.println("[旧订单推送] 更新队列失败 queueId="
                                + erpGoodsOrder.getQueueId() + "，" + e.getMessage());
                    }
                    System.out.println("[旧订单推送] 订单号=" + erpGoodsOrder.getOrderSn() + "，" + msg);
                    return;   // 不下发、不锁库存、不建销售单
                }
            }

            Long autoIssue = warehouseSettings.getAutoIssue() == null ? 1 : warehouseSettings.getAutoIssue();
            if (autoIssue == 0){
                ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
                erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
                erpGoodsOrderQueue.setStatus("3");
                erpGoodsOrderQueue.setMsg("未开启自动下发，需要手动下发");
                erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
            }else{
                GoodsDto goodsDto = null;
                if(erpGoodsOrder.getGoodsDto() == null){
                    // 解析erp订单中商品信息
                    goodsDto = JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class);
                }else{
                    goodsDto = erpGoodsOrder.getGoodsDto();
                }
                if (goodsDto != null){
                    String goodsId = goodsDto.getGoodsId();

                    List<TShopGoodsPublishedDto> tShopGoodsPublishedDtoList = selectByTrilateralId(Long.parseLong(goodsId));
                    if (tShopGoodsPublishedDtoList.isEmpty() && goodsDto.getOuterId() != null){
                        goodsId = goodsDto.getOuterId();
                        try{
                            tShopGoodsPublishedDtoList = selectByTrilateralId(Long.parseLong(goodsId));
                        }catch (NumberFormatException e){
                            System.out.println("非进销存商品");
                        }
                    }
                    if (!tShopGoodsPublishedDtoList.isEmpty()){
                        // 获取库存持有人
                        TShopGoodsPublishedDto tShopGoodsPublishedDto = tShopGoodsPublishedDtoList.get(0);
                        if(tShopGoodsPublishedDto.getUserId() == 0){
                            tShopGoodsPublishedDto.setUserId(erpGoodsOrder.getCreatedBy());
                        }
                        distribution(warehouseSettings,erpGoodsOrder,tShopGoodsPublishedDto);
                    }else{
                        try{
                            // 查询是否
                            String psiRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/shop-goods-published/by-trilateral?user_id="+erpGoodsOrder.getCreatedBy()+"&trilateral_id="+goodsDto.getGoodsId());

                            Map psiResMap = JsonUtil.transferToObj(psiRes,Map.class);
                            List psiData = (List) psiResMap.get("data");
                            if (psiData.isEmpty()){
                                psiRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/shop-goods-published/by-trilateral?user_id="+erpGoodsOrder.getCreatedBy()+"&trilateral_id="+goodsDto.getOuterId());
                                psiResMap = JsonUtil.transferToObj(psiRes,Map.class);
                                if (psiResMap.get("code").toString().equals("200")){
                                    psiData = (List) psiResMap.get("data");
                                }
                            }

                            if (!psiData.isEmpty()){
                                Map data = (Map) psiData.get(0);
                                TShopGoodsPublishedDto tShopGoodsPublishedDto = new TShopGoodsPublishedDto();
                                tShopGoodsPublishedDto.setTrilateralId(Long.parseLong(data.get("trilateral_id").toString()));
                                tShopGoodsPublishedDto.setUserId(erpGoodsOrder.getCreatedBy());
                                tShopGoodsPublishedDto.setProductId(Long.parseLong(data.get("product_id").toString()));
                                tShopGoodsPublishedDto.setIsdistribution("0");
                                distribution(warehouseSettings,erpGoodsOrder,tShopGoodsPublishedDto);
                            }else{
                                ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
                                erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
                                erpGoodsOrderQueue.setStatus("3");
                                erpGoodsOrderQueue.setMsg("异常:未找到发布记录");
                                erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
                            }
                        }catch (Exception e){
                            ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
                            erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
                            erpGoodsOrderQueue.setStatus("3");
                            erpGoodsOrderQueue.setMsg("异常:系统异常请联系管理员");
                            erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
                        }
                    }
                }
            }

        }catch (Exception e){
            System.out.println("订单推送销售订单异常");
            e.printStackTrace();
        }
    }

    /**
     * 进销存 库存同步方法
     * @param productId         进销存商品id
     * @param inventory         新库存
     * @param oldInventory      原库存
     * @param erpGoodsOrder        订单id
     * @return
     */
    @Override
    public String synchronizeStockNew(String productId, Long userId, int inventory, int oldInventory, ErpGoodsOrder erpGoodsOrder, Long skipShopErpId) {
        return doSynchronizeStockNew(productId, userId, inventory, oldInventory, erpGoodsOrder, skipShopErpId);
    }

    @Override
    @DS("taskDb")
    public String synchronizeStockNew(String productId,Long userId,int inventory,int oldInventory,ErpGoodsOrder erpGoodsOrder){
        return doSynchronizeStockNew(productId, userId, inventory, oldInventory, erpGoodsOrder, null);
    }

    public String doSynchronizeStockNew(String productId,Long userId,int inventory,int oldInventory,ErpGoodsOrder erpGoodsOrder, Long skipShopErpId){
        String log = "库存同步操作记录：";
        // 根据商品id获取店铺关联信息
        List<TShopGoodsPublishedDto> shopGoodsPublishedDtoList = new ArrayList<>();

        // 查询是否
        // 注意：必须用商品所有者（分销场景为货主/仓库方）查询，不能用下单人，
        // 否则会查到下单人自己租户里同 product_id 的记录，导致货主自营店铺不再被同步
        Long stockOwnerId = userId != null ? userId : erpGoodsOrder.getCreatedBy();
        String psiRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/shop-goods-published/by-product-id?user_id="+stockOwnerId+"&product_id="+productId);
        Map psiResMap = JsonUtil.transferToObj(psiRes,Map.class);
        List psiData = (List) psiResMap.get("data");


        if (!psiData.isEmpty()){
            for (int i=0;i<psiData.size();i++){
                Map data = (Map) psiData.get(i);
                TShopGoodsPublishedDto tShopGoodsPublishedDto = new TShopGoodsPublishedDto();
                tShopGoodsPublishedDto.setTrilateralId(Long.parseLong(data.get("trilateral_id").toString()));
                tShopGoodsPublishedDto.setUserId(stockOwnerId);
                tShopGoodsPublishedDto.setProductId(Long.parseLong(data.get("product_id").toString()));
                tShopGoodsPublishedDto.setIsdistribution("0");
                tShopGoodsPublishedDto.setErpShopId(Long.parseLong(data.get("shop_erp_id").toString()));
                shopGoodsPublishedDtoList.add(tShopGoodsPublishedDto);
            }
        }

        // 类内部调用不会经过 @DS 代理，这里显式切到 taskDb（t_shop_goods_published 在 task 库）
        boolean needTaskDb = !"taskDb".equals(DynamicDataSourceContextHolder.peek());
        if (needTaskDb){
            DynamicDataSourceContextHolder.push("taskDb");
        }
        try{
            if (shopGoodsPublishedDtoList.isEmpty()){
                shopGoodsPublishedDtoList = selectByProductId(Long.parseLong(productId),userId,"0");
            }
            // 补齐分销数据
            List<TShopGoodsPublishedDto> shopGoodsPublishedFenXiaoList = selectByProductId(Long.parseLong(productId),userId,"1");
            if (!shopGoodsPublishedFenXiaoList.isEmpty()){
                shopGoodsPublishedDtoList.addAll(shopGoodsPublishedFenXiaoList);
            }
        }finally{
            if (needTaskDb){
                DynamicDataSourceContextHolder.poll();
            }
        }

        // 两源合并后按店铺去重：同一店铺只处理一次，task 表的分销行优先（带 is_distribution 与归属）
        java.util.Map<Long, TShopGoodsPublishedDto> dedupMap = new java.util.LinkedHashMap<>();
        for (TShopGoodsPublishedDto dto : shopGoodsPublishedDtoList) {
            if (dto.getErpShopId() == null) {
                    continue;
            }
            TShopGoodsPublishedDto exist = dedupMap.get(dto.getErpShopId());
            if (exist == null || "1".equals(dto.getIsdistribution())) {
                    dedupMap.put(dto.getErpShopId(), dto);
                }
        }
        shopGoodsPublishedDtoList = new java.util.ArrayList<>(dedupMap.values());
        // 如果已发布记录不存在
        if(shopGoodsPublishedDtoList.isEmpty()){
            // 记录日志
            log += "无已发布记录";
        }else{
            // 提取所有 erpShopId 并去重，生成 Long 类型集合（用于批量查询）
            List<Long> erpShopIdList = shopGoodsPublishedDtoList.stream()
                    // 提取erpShopId字段
                    .map(TShopGoodsPublishedDto::getErpShopId)
                    // 过滤掉 null 值，避免空指针
                    .filter(Objects::nonNull)
                    // 去重（同一个店铺ID只保留一个）
                    .distinct()
                    // 转为List集合
                    .collect(Collectors.toList());
            List<Shop> shopList = shopService.selectBatchByIds(erpShopIdList);
            java.util.Map<Long, Shop> shopMap = new java.util.HashMap<>();
            for (Shop s : shopList) {
                shopMap.put(s.getId(), s);
            }
            // 分销单库存同步日志脱敏：订单号用ERP分销单号、店铺显示ERP分销店铺、金额用仓库价（不含运费）
            String[] syncLogJson = buildDistributionSyncLog(JsonUtil.transferToJson(erpGoodsOrder), erpGoodsOrder.getItemList(), productId, stockOwnerId);
//            List<SynchronizationShopLog> synchronizationShopLogsList = new ArrayList<>();
            // 循环已发布商品记录，将所有已发布商品进行库存同步
            for (TShopGoodsPublishedDto sgp : shopGoodsPublishedDtoList){
                if (Objects.equals(sgp.getErpShopId(), erpGoodsOrder.getShopErpId())){
                    continue;
                }
                if (skipShopErpId != null && Objects.equals(sgp.getErpShopId(), skipShopErpId)) {
                    continue;
                }
                Map synchronizationShopLog = new HashMap();
                synchronizationShopLog.put("product_id",sgp.getProductId());
                synchronizationShopLog.put("product_user_id",sgp.getUserId());
                synchronizationShopLog.put("erp_order", syncLogJson[0]);
                synchronizationShopLog.put("platform", syncLogJson[1]);
                if (inventory < 0){
                    synchronizationShopLog.put("update_type","扣减库存");
                }else{
                    synchronizationShopLog.put("update_type","增加库存");
                }
                Shop shop = shopMap.get(sgp.getErpShopId());
                if (shop == null) {
                    // 店铺信息统一取自 zhishu.t_shop：查不到即视为不存在或已删除
                    writeSyncSkipLog(synchronizationShopLog, sgp, null, "店铺不存在或已删除");
                    log += "店铺ID：" + sgp.getErpShopId() + "[" + shopTag(sgp, null) + "]：跳过（店铺不存在或已删除）;";
                    continue;
                }
                String skipReason = checkShopSyncValid(shop);
                if (skipReason != null) {
                    writeSyncSkipLog(synchronizationShopLog, sgp, shop, skipReason);
                    log += "店铺：" + shop.getShopName() + "(" + shop.getId() + ")[" + shopTag(sgp, shop) + "]：跳过（" + skipReason + "）;";
                    continue;
                }
                {
                    try {
                        // 店铺id
                        synchronizationShopLog.put("shop_id",shop.getId());
                        // 店铺名称
                        synchronizationShopLog.put("shop_name",shop.getShopName());
                        // 店铺类型
                        synchronizationShopLog.put("shop_type",shop.getShopType());
                        // 店铺创建人
                        synchronizationShopLog.put("shop_create_by",shop.getCreateBy());
                        String addRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/synchronization-shop-log/save",synchronizationShopLog);
                        System.out.println("库存操作日志记录添加记录："+addRes);
                        Map addResMap = JsonUtil.transferToObj(addRes,Map.class);
                        Map addResDataMap = (Map) addResMap.get("data");
                        Map synchronizationShopLogUpdate = new HashMap();
                        synchronizationShopLogUpdate.put("shop_create_by",shop.getCreateBy());
                        synchronizationShopLogUpdate.put("id",addResDataMap.get("id").toString());
                        Map resultMap = new HashMap();
                        resultMap.put("code","500");
                        resultMap.put("msg","未支持的店铺类型："+shop.getShopType());
                        if(shop.getShopType().equals("1")){
                            // 获取库存
                            String pddRes = InterfaceUtils.getInterface("http://pdd.buzhiyushu.cn","/api/pdd/auth/getShopGoodsDetail?accessToken="+shop.getToken()+"&goodsId="+sgp.getTrilateralId());
                            Map pddResMap = JsonUtil.transferToObj(pddRes,Map.class);
                            if (pddResMap.get("error_response") == null){
                                Map goodsDetailGetResponse = (Map) pddResMap.get("goods_detail_get_response");
                                List skuList = (List) goodsDetailGetResponse.get("sku_list");
                                Map skuMap = (Map) skuList.get(0);
                                String skuId = skuMap.get("sku_id").toString();
                                BigDecimal stock = new BigDecimal(skuMap.get("quantity").toString());
                                // 如果psi的库存比平台的库存少，那么用psi库存
                                if (stock.compareTo(new BigDecimal(oldInventory)) > 0){
                                    stock = new BigDecimal(oldInventory);
                                }
                                // 更新后库存
                                BigDecimal inventoryNew = stock.add(new BigDecimal(inventory));
                                // 记录日志
                                synchronizationShopLogUpdate.put("quantity",Math.abs(inventory)+"");
                                synchronizationShopLogUpdate.put("inventory",inventoryNew.toString());
                                synchronizationShopLogUpdate.put("inventory_old",stock.toString());
                                // 调用拼多多修改库存
                                resultMap = editStockService.pddEditStockNew(shop,sgp.getTrilateralId().toString(),inventoryNew+"",1,skuId);
                                log += "拼多多店铺："+shop.getShopName() +":"+resultMap.get("msg")+";";
                            }else{
                                Map errorResponse = (Map) pddResMap.get("error_response");
                                String errorMsg = errorResponse.get("error_msg").toString();
                                if (errorMsg.equals("access_token已过期")){
                                    errorMsg = "店铺失效，请重新授权";
                                }else{
                                    errorMsg += ";" + errorResponse.get("sub_msg");
                                }
                                resultMap = new HashMap();
                                resultMap.put("code","500");
                                resultMap.put("msg",errorMsg);
                                log += "拼多多店铺："+shop.getShopName() +":" + errorMsg + ";";
                            }

                        } else if (shop.getShopType().equals("2")){
                            // 获取库存
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("itemId",sgp.getTrilateralId().toString());
                            String res = KfzSimpleDllLoader.executeKongfzShopItemListFunc(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,shop.getToken(),jsonObject.toString());
                            Map resMap = JsonUtil.transferToObj(res,Map.class);
                            Map successResponse = resMap.get("successResponse") == null ? null : (Map) resMap.get("successResponse");
                            if (successResponse != null){
                                List dataList = (List) successResponse.get("list");
                                if (!dataList.isEmpty()){
                                    Map data = (Map) dataList.get(0);
                                    BigDecimal stock = new BigDecimal(data.get("number").toString());
                                    // 如果psi的库存比平台的库存少，那么用psi库存
                                    if (stock.compareTo(new BigDecimal(oldInventory)) > 0){
                                        stock = new BigDecimal(oldInventory);
                                    }
                                    // 更新后库存
                                    BigDecimal inventoryNew = stock.add(new BigDecimal(inventory));
                                    // 记录日志
                                    synchronizationShopLogUpdate.put("quantity",Math.abs(inventory)+"");
                                    synchronizationShopLogUpdate.put("inventory",inventoryNew.toString());
                                    synchronizationShopLogUpdate.put("inventory_old",stock.toString());
                                    // 调用孔夫子修改库存
                                    resultMap = editStockService.kfzEditStock(shop.getToken(),sgp.getTrilateralId().toString(),inventoryNew+"");
                                    log += "孔夫子店铺："+shop.getShopName() +":"+resultMap.get("msg")+";";
                                }else{
                                    resultMap.put("code","500");
                                    resultMap.put("msg","未查询到商品信息");
                                    log += "孔夫子店铺："+shop.getShopName() +":未查询到商品信息;";
                                }
                            }else{
                                Map errorResponse = (Map) resMap.get("errorResponse");
                                String msg = errorResponse.get("msg").toString();
                                if (msg.contains("accessToken")){
                                    msg = "店铺失效，请重新授权";
                                }

                                resultMap = new HashMap();
                                resultMap.put("code","500");
                                resultMap.put("msg",msg);
                                log += "孔夫子店铺："+shop.getShopName() +":" + msg + ";";
                            }
                        } else if (shop.getShopType().equals("5")){
                            // 查询店铺库存
                            JSONObject xyGoodsMap = new JSONObject();
                            // 闲管家商品id
                            xyGoodsMap.put("product_id",sgp.getTrilateralId());
                            // appId
                            xyGoodsMap.put("appId",shop.getMallId());
                            // appSecret
                            xyGoodsMap.put("appSecret",shop.getToken());
                            // user_name
                            String[] userNames = new String[]{shop.getShopKey()};
                            xyGoodsMap.put("user_name",userNames);
                            String res = DllInitializer.executeGetGoodsDetail(xyGoodsMap.toString());
                            Map resMap = JsonUtil.transferToObj(res,Map.class);
                            if (resMap.get("msg").toString().equals("OK")){
                                Map dataMap = (Map) resMap.get("data");
                                BigDecimal stock = new BigDecimal(dataMap.get("stock").toString());
                                // 闲管家上下架状态：publish_status 3=已上架 4=已下架；下架后 stock 仍报原值，不能直接叠加
                                boolean offShelf = dataMap.get("publish_status") != null
                                        && "4".equals(dataMap.get("publish_status").toString());
                                if (offShelf && inventory > 0) {
                                    // 因库存扣到 0 被下架：回滚只需重新上架，库存保持平台当前值，不再叠加
                                    synchronizationShopLogUpdate.put("quantity",Math.abs(inventory)+"");
                                    synchronizationShopLogUpdate.put("inventory",stock.toString());
                                    synchronizationShopLogUpdate.put("inventory_old",stock.toString());
                                    resultMap = editStockService.xyEditStock(shop,sgp.getTrilateralId().toString(),stock.toString());
                                    log += "闲鱼店铺："+shop.getShopName() +":商品处于下架状态，直接上架（库存"+stock+"）:"+resultMap.get("msg")+";";
                                } else {
                                    // 如果psi的库存比平台的库存少，那么用psi库存
                                    if (stock.compareTo(new BigDecimal(oldInventory)) > 0){
                                        stock = new BigDecimal(oldInventory);
                                    }
                                    // 更新后库存
                                    BigDecimal inventoryNew = stock.add(new BigDecimal(inventory));
                                    // 记录日志
                                    synchronizationShopLogUpdate.put("quantity",Math.abs(inventory)+"");
                                    synchronizationShopLogUpdate.put("inventory",inventoryNew.toString());
                                    synchronizationShopLogUpdate.put("inventory_old",stock.toString());
                                    // 调用闲鱼修改库存（库存>0 时内部会自动上架）
                                    resultMap = editStockService.xyEditStock(shop,sgp.getTrilateralId().toString(),inventoryNew+"");
                                    log += "闲鱼店铺："+shop.getShopName() +":"+resultMap.get("msg")+";原库存："+stock+";现库存："+inventoryNew;
                                }
                            }else{

                                resultMap = new HashMap();
                                resultMap.put("code","500");
                                resultMap.put("msg",resMap.get("msg"));

                                log += "闲鱼店铺："+shop.getShopName() +";查询商品异常："+resMap.get("msg");
                            }
                        } else {
                            // 淘宝(6)等暂不支持的平台：跳过，不阻塞其他店铺
                            resultMap.put("code","200");
                            resultMap.put("msg","该平台暂不支持库存同步，已跳过");
                            log += "店铺：" + shop.getShopName() + "：该平台暂不支持库存同步，已跳过;";
                        }
                        // 状态码
                        synchronizationShopLogUpdate.put("code",resultMap.get("code") == null ? "500" : resultMap.get("code").toString());
                        // 日志
                        String msg = "";
                        try{
                            msg = resultMap.get("msg").toString();
                            if(msg.contains("http")){
                                msg = "接口调用异常，请联系管理员";
                            }
                        }catch (Exception e){
                            msg = "接口调用异常，请联系管理员";
                        }
                        synchronizationShopLogUpdate.put("msg",msg);
                        String res = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/synchronization-shop-log/update",synchronizationShopLogUpdate);
                        System.out.println("修改库存日志："+res);
                        continue;
                    } catch (Exception e) {
                        log += (shop.getShopName() == null ? "" : shop.getShopName()) + "：库存同步异常：" + e.getMessage() + ";";
                    }
                    }
            }
        }

        // 返回日志信息
        return log;
    }



    /** 店铺是否可参与库存同步：返回 null 表示可同步，否则返回跳过原因 */
    private String checkShopSyncValid(Shop shop) {
        if (shop == null) {
            return "店铺不存在或已删除";
        }
        if ("1".equals(shop.getDelFlag())) {
            return "店铺已删除";
        }
        if (shop.getShopAuthorize() != null && !"1".equals(shop.getShopAuthorize())) {
            return "0".equals(shop.getShopAuthorize()) ? "店铺未授权" : "店铺授权已过期";
        }
        java.util.Date now = new java.util.Date();
        if (shop.getExpirationTime() != null && shop.getExpirationTime().before(now)) {
            return "店铺授权已过期";
        }
        if (shop.getShopExpirationTime() != null && shop.getShopExpirationTime().before(now)) {
            return "店铺订阅已过期";
        }
        if ("1".equals(shop.getStatus())) {
            return "店铺已停用";
        }
        return null;
    }

    /** 店铺归属标签：自营 / 分销（带分销商ID） */
    private String shopTag(TShopGoodsPublishedDto sgp, Shop shop) {
        if ("1".equals(sgp.getIsdistribution())) {
            String owner = shop != null && shop.getCreateBy() != null ? shop.getCreateBy() : String.valueOf(sgp.getUserId());
            return "分销 分销商:" + owner;
        }
        return "自营";
    }

    /** 写一条「跳过同步」日志，不调用平台接口 */
    private void writeSyncSkipLog(java.util.Map baseLog, TShopGoodsPublishedDto sgp, Shop shop, String reason) {
        try {
            java.util.Map<String, Object> save = new java.util.HashMap<>(baseLog);
            save.put("shop_id", sgp.getErpShopId());
            save.put("shop_name", shop != null ? shop.getShopName() : ("店铺" + sgp.getErpShopId()));
            save.put("shop_type", shop != null ? shop.getShopType() : "0");
            Object createBy = shop != null && shop.getCreateBy() != null ? shop.getCreateBy() : sgp.getUserId();
            save.put("shop_create_by", createBy);
            String addRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/synchronization-shop-log/save", save);
            java.util.Map addResMap = JsonUtil.transferToObj(addRes, java.util.Map.class);
            java.util.Map addResDataMap = (java.util.Map) addResMap.get("data");
            if (addResDataMap == null || addResDataMap.get("id") == null) {
                return;
            }
            java.util.Map<String, Object> upd = new java.util.HashMap<>();
            upd.put("id", addResDataMap.get("id").toString());
            upd.put("shop_create_by", createBy);
            upd.put("code", "500");
            upd.put("msg", "跳过：" + reason);
            InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/synchronization-shop-log/update", upd);
            System.out.println("库存同步跳过日志：" + sgp.getErpShopId() + " " + reason);
        } catch (Exception e) {
            System.out.println("写库存同步跳过日志失败：" + e.getMessage());
        }
    }
    /**
     * 孔夫子库存修改
     * @param trilateralId
     * @param goodsCount
     * @return
     */
    public String kfzStockEdit(ErpGoodsOrder erpGoodsOrder ,String trilateralId,int inventory){
        List<TShopGoodsPublishedDto> shopGoodsPublishedDtoList = selectByTrilateralId(Long.parseLong(trilateralId));
        TShopGoodsPublishedDto sgp = shopGoodsPublishedDtoList.get(0);
        Shop shop = shopService.queryById(sgp.getErpShopId());
        Map synchronizationShopLog = new HashMap();
        synchronizationShopLog.put("product_id",sgp.getProductId());
        synchronizationShopLog.put("product_user_id",sgp.getUserId());
        synchronizationShopLog.put("erp_order",JsonUtil.transferToJson(erpGoodsOrder));
        synchronizationShopLog.put("platform",erpGoodsOrder.getItemList());
        if (inventory < 0){
            synchronizationShopLog.put("update_type","扣减库存");
        }else{
            synchronizationShopLog.put("update_type","增加库存");
        }
        // 店铺id
        synchronizationShopLog.put("shop_id",shop.getId());
        // 店铺名称
        synchronizationShopLog.put("shop_name",shop.getShopName());
        // 店铺类型
        synchronizationShopLog.put("shop_type",shop.getShopType());
        // 店铺创建人
        synchronizationShopLog.put("shop_create_by",shop.getCreateBy());
        String addRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/synchronization-shop-log/save",synchronizationShopLog);
        System.out.println("库存操作日志记录添加记录："+addRes);
        Map addResMap = JsonUtil.transferToObj(addRes,Map.class);
        Map addResDataMap = (Map) addResMap.get("data");
        Map synchronizationShopLogUpdate = new HashMap();
        synchronizationShopLogUpdate.put("shop_create_by",shop.getCreateBy());
        synchronizationShopLogUpdate.put("id",addResDataMap.get("id").toString());
        Map resultMap = new HashMap();
        String log = "";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("itemId",trilateralId);
        String res = KfzSimpleDllLoader.executeKongfzShopItemListFunc(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET,shop.getToken(),jsonObject.toString());
        Map resMap = JsonUtil.transferToObj(res,Map.class);
        Map successResponse = resMap.get("successResponse") == null ? null : (Map) resMap.get("successResponse");
        if (successResponse != null){
            List dataList = (List) successResponse.get("list");
            if (!dataList.isEmpty()){
                Map data = (Map) dataList.get(0);
                BigDecimal stock = new BigDecimal(data.get("number").toString());
                // 更新后库存
                BigDecimal inventoryNew = stock.add(new BigDecimal(inventory));
                // 记录日志
                synchronizationShopLogUpdate.put("quantity",Math.abs(inventory)+"");
                synchronizationShopLogUpdate.put("inventory",inventoryNew.toString());
                synchronizationShopLogUpdate.put("inventory_old",stock.toString());
                // 调用孔夫子修改库存
                resultMap = editStockService.kfzEditStock(shop.getToken(),trilateralId,inventoryNew+"");
                log += "孔夫子店铺："+shop.getShopName() +":"+resultMap.get("msg")+";";
            }else{
                resultMap.put("code","500");
                resultMap.put("msg","未查询到商品信息");
                log += "孔夫子店铺："+shop.getShopName() +":未查询到商品信息;";

            }
        }else{
            Map errorResponse = (Map) resMap.get("errorResponse");
            String msg = errorResponse.get("msg").toString();
            if (msg.contains("accessToken")){
                msg = "店铺失效，请重新授权";
            }
            resultMap = new HashMap();
            resultMap.put("code","500");
            resultMap.put("msg",msg);
            log += "孔夫子店铺："+shop.getShopName() +":" + msg + ";";
        }
        synchronizationShopLogUpdate.put("code",resultMap.get("code") == null ? "500" : resultMap.get("code").toString());
        // 日志
        String msg = "";
        try{
            msg = resultMap.get("msg").toString();
            if(msg.contains("http")){
                msg = "接口调用异常，请联系管理员";
            }
        }catch (Exception e){
            msg = "接口调用异常，请联系管理员";
        }
        synchronizationShopLogUpdate.put("msg",msg);
        String resData = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/synchronization-shop-log/update",synchronizationShopLogUpdate);
        System.out.println("修改库存日志："+resData);
        return log;
    }


    @Override
    public Map createSalesOrder(String orderId,String orderSn,String productId,String unitPrice,
                                 String quantity,String sales_person,
                                 String sales_person_id,String about_id,String shopType,String receiverName,String receiverPhone,String receiverAddress) {


        Map<String, String> requestParams = new HashMap<>();
        // 添加签名相关参数
        requestParams.put("app_key", "psi");
        requestParams.put("client_id", "psi");
        requestParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        requestParams.put("sign_method", "md5");
        // 添加业务参数（从对象中获取）
        //关联订单id（平台ID）
        requestParams.put("association_order_id",orderId);
        // 订单编号
        requestParams.put("association_order_no", orderSn);
        // 来源类型 0-预留 1-erp订单
        requestParams.put("from_type", "1");
        // 商品id
        requestParams.put("items[0][product_id]",productId);
        // 单价
        requestParams.put("items[0][unit_price]", unitPrice);
        // 数量
        requestParams.put("items[0][quantity]", quantity);
        // 店铺名
        requestParams.put("sales_person", sales_person);
        // 店铺id
        requestParams.put("sales_person_id", sales_person_id);
        // 店铺创建id
        requestParams.put("about_id", about_id);
        // 店铺类型
        requestParams.put("shop_type",shopType);
        // 收货人姓名
        requestParams.put("receiver_name",receiverName);
        // 收货人电话
        requestParams.put("receiver_phone",receiverPhone);
        // 收货地址
        requestParams.put("receiver_address",receiverAddress);

        // 调用远程接口
        String result = InterfaceUtils.postFormWithSign(
                UrlUtil.getNewWarehouse()+"/api/sales-order/create",
                null,
                requestParams,
                "",
                "md5"
        );
        Map resultMap = JsonUtil.transferToObj(result, Map.class);
        return resultMap;
    }



    /**
     * 分销
     */
    @Transactional(rollbackFor = Exception.class)
    public void distribution(WarehouseSettings warehouseSettings, ErpGoodsOrder erpGoodsOrder, TShopGoodsPublishedDto tShopGoodsPublishedDto){
        // 平台账号信息
        SysUser adminUser = userService.selectUserOne(1L);
        // 商品信息
        GoodsDto goodsDto = erpGoodsOrder.getGoodsDto() == null ? JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class) : erpGoodsOrder.getGoodsDto();
        // 下单数量
        int goodsCount = Integer.parseInt(goodsDto.getGoodsCount());
        // 店铺类型
        Long shopType = erpGoodsOrder.getShopType();
        // 获取商品id
        String goodsId = "-1";
        if(shopType == 5){
            goodsId = goodsDto.getOuterId();        //闲鱼
        }else if(shopType == 1|| shopType == 2){
            goodsId = goodsDto.getGoodsId();        //拼多多
        }
        // 分销标记
        String isdistribution = tShopGoodsPublishedDto.getIsdistribution();
        // 商品id不为空的情况
        if (!StringUtils.isEmpty(goodsId) &&  !goodsId.equals("-1")){
            // 校验是否存在不匹配规则，如果不匹配则直接下发
            Boolean matchMark = true;
            // ISBN 优先匹配标记
            Boolean isbnMatchMark = false;
            // 发货地 省
            String senderProv = "";
            // 品相
            String conditionCode = "";
            if (warehouseSettings.getUserSettingsAttributeList()!= null && !warehouseSettings.getUserSettingsAttributeList().isEmpty()){
                for(UserSettingsAttribute userSettingsAttribute : warehouseSettings.getUserSettingsAttributeList()){
                    // 存在不匹配规则
                    if(userSettingsAttribute.getAttributeId() == 7){
                        matchMark = false;
                        break;
                    }
                    // isbn 优先匹配
                    if(userSettingsAttribute.getAttributeId() == 1 && userSettingsAttribute.getAttributeValue().equals("1")){
                        isbnMatchMark = true;
                    }else if (userSettingsAttribute.getAttributeId() == 4){
                        // 发货地 省
                        senderProv = userSettingsAttribute.getAttributeValue();
                    }else if (userSettingsAttribute.getAttributeId() == 6){
                        // 品相
                        conditionCode = userSettingsAttribute.getAttributeValue();
                    }
                }
            }else{
                matchMark = false;
            }
            // 先查询指定商品信息
            String productRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product/full_info?user_id="+tShopGoodsPublishedDto.getUserId()+"&product_id="+tShopGoodsPublishedDto.getProductId());
            Map productResMap = JsonUtil.transferToObj(productRes,Map.class);
            Map psiProduct = new HashMap();
            if (productResMap.get("code").toString().equals("200")){
                // 获取商品对象
                psiProduct = (Map) productResMap.get("data");
                psiProduct.put("about_id",tShopGoodsPublishedDto.getUserId().toString());
                // 获取ISBN
                String isbn = psiProduct.get("barcode") == null ? "" : psiProduct.get("barcode").toString();
                // 校验是否是无书号书籍
                if (!isbn.startsWith("9787")){
                    //非9787开头的就是无ISBN书籍， 如果是无书号商品，则不进行重新匹配商品，上的是谁的就下发给谁
                    matchMark = false;
                }
                // 获取店铺用户余额
                SysUser sysUser = userService.selectUserOne(erpGoodsOrder.getCreatedBy());
                BigDecimal balance = sysUser.getBalance();  // 余额
                /**
                 * 获取分账配置  若未配置则走默认设置
                 */
                PsiEmployees psiEmployees = psiEmployeesService.selectOneByAboutIdAndPhone(sysUser.getUserId(),sysUser.getPhonenumber());
                // 仓库方
                PsiSplitAccountConfig psiSplitAccountConfigWarehouse = new PsiSplitAccountConfig();
                // 分润方
                PsiSplitAccountConfig psiSplitAccountConfigPlatform = new PsiSplitAccountConfig();
                if (psiEmployees != null && psiEmployees.getRuleValue() != null){
                    List ruleValueList = JsonUtil.transferToObj(psiEmployees.getRuleValue(),List.class);

                    for (Object ruleValue : ruleValueList){
                        Map ruleValueMap = (Map) ruleValue;
                        if (ruleValueMap.get("product_type").equals("仓库方")){
                            psiSplitAccountConfigWarehouse.setProductType(ruleValueMap.get("product_type").toString());
                            psiSplitAccountConfigWarehouse.setRatio(new BigDecimal(ruleValueMap.get("ratio").toString()));
                            psiSplitAccountConfigWarehouse.setAddAmount(new BigDecimal(ruleValueMap.get("add_amount").toString()).multiply(new BigDecimal(100)));
                        }else if(ruleValueMap.get("product_type").equals("分润方")){
                            psiSplitAccountConfigPlatform.setProductType(ruleValueMap.get("product_type").toString());
                            psiSplitAccountConfigPlatform.setRatio(new BigDecimal(ruleValueMap.get("ratio").toString()));
                            psiSplitAccountConfigPlatform.setAddAmount(new BigDecimal(ruleValueMap.get("add_amount").toString()).multiply(new BigDecimal(100)));
                        }
                    }
                }else{
                    psiEmployees = new PsiEmployees();
                    // 默认模板id
                    psiEmployees.setSplitAccountConfigId(1L);
                    psiEmployees.setRuleName("默认规则");
                    // 未查询到则执行 默认配置 双向都收    3% + 0.1
                    psiSplitAccountConfigWarehouse.setProductType("仓库方");
                    psiSplitAccountConfigWarehouse.setRatio(new BigDecimal("0.03"));
                    psiSplitAccountConfigWarehouse.setAddAmount(new BigDecimal("0.1").multiply(new BigDecimal(100)));
                    psiSplitAccountConfigPlatform.setProductType("分润方");
                    psiSplitAccountConfigPlatform.setRatio(new BigDecimal("0.03"));
                    psiSplitAccountConfigPlatform.setAddAmount(new BigDecimal("0.1").multiply(new BigDecimal(100)));
                }
                // 防重复执行校验：根据订单ID+平台商品ID缓存判断
                // 【优化 2026-10-19】原实现把校验放在 goodsCount 循环体内，TTL 只有 100ms，
                // 必须靠 Thread.sleep(100) 让 key 过期才能继续处理多本订单（既慢又不可靠）。
                // 现改为整个订单处理前只校验一次，TTL 提到 30 秒，并去掉 sleep。
                String dedupKey = erpGoodsOrder.getId() + ":" + goodsId;
                if (redisService.get(dedupKey) != null) {
                    updateQueueStatus(erpGoodsOrder, "2", "失败：重复异常执行");
                    return;
                }
                redisService.set(dedupKey, "1", 30, TimeUnit.SECONDS);

                // 循环库存数量
                for (int i=0;i<goodsCount;i++){


                    // 是否重新匹配
                    if (matchMark){
                        // 若需要重新匹配商品，则需要重新查询商品
                        String matchRes =  InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product_book/list?page=1&page_size=100&isbn="+isbn);
                        Map matchResMap = JsonUtil.transferToObj(matchRes,Map.class);
                        if(matchResMap.get("code") != null && matchResMap.get("code").toString().equals("200")){
                            Map dataMap = (Map) matchResMap.get("data");
                            List matchGoodsList = (List) dataMap.get("list");
                            if (matchGoodsList != null && !matchGoodsList.isEmpty()){
                                for (Object object : matchGoodsList){
                                    Map matchGoods = (Map) object;
                                    // 获取匹配商品库存和运费模板
                                    String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product/getProductInventory?user_id="+psiProduct.get("about_id")+"&product_id="+psiProduct.get("id")+"&type=1");
                                    // 获取运费
                                    BigDecimal cost = getCost(erpGoodsOrder,stockRes);
                                    // 仓库价格
                                    BigDecimal warehousePrice = new BigDecimal(matchGoods.get("sale_price").toString()).add(cost);
                                    // 分润方  手续费  （书价（商品价格 + 运费） * 分润方百分比）  +  分润发加价
                                    BigDecimal handlingFeePlatform = warehousePrice.multiply(psiSplitAccountConfigPlatform.getRatio()).setScale(0, RoundingMode.CEILING).add(psiSplitAccountConfigPlatform.getAddAmount());
                                    // 总价
                                    BigDecimal totalPrice = warehousePrice.add(handlingFeePlatform);
                                    // 订单金额
                                    BigDecimal orderPrice = new BigDecimal(erpGoodsOrder.getOrderTotal());
                                    // 差价
                                    BigDecimal differencePrice = orderPrice.subtract(totalPrice);
                                    // 差价 大于 亏损保护的金额 或者 是自营商品
                                    if (differencePrice.compareTo(warehouseSettings.getProfitFloor()) >= 0 || matchGoods.get("about_id").toString().equals(erpGoodsOrder.getCreatedBy().toString())){
                                        // 校验aboutId  是否是同一个人，校验是否需要分销
                                        if (matchGoods.get("about_id").toString().equals(erpGoodsOrder.getCreatedBy().toString())){
                                            isdistribution = "0";
                                        }else{
                                            isdistribution = "1";
                                        }
                                        // 获取第一条数据的商品信息 赋值到商品信息对象里
                                        // 用户id
                                        String aboutId = matchGoods.get("about_id").toString();
                                        // 获取商品id
                                        String productId = matchGoods.get("self_id").toString();
                                        // 查询商品信息
                                        String productMatchRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product/full_info?user_id="+aboutId+"&product_id="+productId);
                                        // 转换格式
                                        Map productMatchResMap = JsonUtil.transferToObj(productMatchRes,Map.class);
                                        // 如果成功，则使用新获取的商品
                                        if (productMatchResMap.get("code").toString().equals("200")){
                                            psiProduct = new HashMap();
                                            psiProduct = (Map) productMatchResMap.get("data");
                                            psiProduct.put("about_id",aboutId);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 获取库存
                    String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product/getProductInventory?user_id="+psiProduct.get("about_id")+"&product_id="+psiProduct.get("id")+"&type=1");
                    Map stockResMap = JsonUtil.transferToObj(stockRes,Map.class);
                    Map stcokData = stockResMap == null ? null : (Map) stockResMap.get("data");
                    if (stcokData == null) {
                        stcokData = new HashMap();
                    }
                    // 原始库存
                    BigDecimal quantity = BigDecimal.ZERO;
                    if (stcokData.get("quantity") != null) {
                        try {
                            quantity = new BigDecimal(stcokData.get("quantity").toString());
                        } catch (Exception e) {
                            quantity = BigDecimal.ZERO;
                        }
                    }
                    // 根据收货地获取收费
                    BigDecimal cost = BigDecimal.ZERO;
                    try {
                        cost = getCost(erpGoodsOrder, stockRes);
                    } catch (Exception e) {
                        System.out.println("[下发PSI] 运费解析失败 orderSn=" + erpGoodsOrder.getOrderSn() + " err=" + e.getMessage());
                    }
                    psiProduct.put("cost", cost);
                    // 商品价格
                    BigDecimal salePrice = toDecimal(psiProduct.get("sale_price"));
                    // 书价（商品价格 + 运费）
                    BigDecimal price = salePrice.add(cost);
                    // 分润方 手续费
                    BigDecimal handlingFeePlatform = price.multiply(psiSplitAccountConfigPlatform.getRatio()).setScale(0, RoundingMode.CEILING).add(psiSplitAccountConfigPlatform.getAddAmount());
                    // 仓库方 手续费
                    BigDecimal handlingFeeWarehouse = price.multiply(psiSplitAccountConfigWarehouse.getRatio()).setScale(0, RoundingMode.CEILING).add(psiSplitAccountConfigWarehouse.getAddAmount());
                    // 总价（应付）
                    BigDecimal totalPrice = price.add(handlingFeePlatform);
                    // 订单金额（买家实付）
                    BigDecimal orderPrice = new BigDecimal(erpGoodsOrder.getOrderTotal());
                    // 差价
                    BigDecimal differencePrice = orderPrice.subtract(totalPrice);
                    // 是否分销：商品货主与下单人不一致即为分销
                    Object aboutIdObj = psiProduct.get("about_id");
                    boolean isDistribution = aboutIdObj != null
                            && !aboutIdObj.toString().equals(String.valueOf(erpGoodsOrder.getCreatedBy()));

                    // ===== 分销订单缓冲层：先建 ERP 分销订单，再做校验；任何失败都留单 =====
                    ErpDistributionOrder erpOrder = null;
                    if (isDistribution) {
                        erpOrder = prepareErpDistributionOrder(
                                erpGoodsOrder, psiProduct, goodsDto, i + 1,
                                salePrice, cost, price, handlingFeePlatform, handlingFeeWarehouse, totalPrice,
                                psiEmployees, psiSplitAccountConfigWarehouse, psiSplitAccountConfigPlatform);
                        if (erpOrder == null || erpOrder.getId() == null) {
                            updateQueueStatus(erpGoodsOrder, "2", "失败：创建ERP分销订单失败");
                            continue;
                        }
                        Integer erpOrderStatus = erpOrder.getStatus() == null ? ErpDistributionOrder.STATUS_PENDING : erpOrder.getStatus();
                        if (erpOrderStatus == ErpDistributionOrder.STATUS_PSI_OK || erpOrderStatus == ErpDistributionOrder.STATUS_FINISHED) {
                            updateQueueStatus(erpGoodsOrder, "1", "订单已下发（" + erpOrder.getOrderNo() + "）");
                            continue;
                        }
                        if (erpOrderStatus != ErpDistributionOrder.STATUS_PENDING && erpOrderStatus != ErpDistributionOrder.STATUS_FAIL_UNSPLIT) {
                            updateQueueStatus(erpGoodsOrder, "2", "失败：ERP分销订单状态" + erpOrderStatus + "，请到分销订单页处理（" + erpOrder.getOrderNo() + "）");
                            continue;
                        }
                    }

                    // 校验1：亏损保护（自营不受限制，关闭亏损保护也放行）
                    if (!(differencePrice.compareTo(warehouseSettings.getProfitFloor()) >= 0
                            || !isDistribution
                            || warehouseSettings.getLossProtection() == 0)) {
                        String failMsg = "亏损保护：差价" + differencePrice.toPlainString() + "分，低于利润下限" + warehouseSettings.getProfitFloor().toPlainString() + "分";
                        markDistributionFail(erpOrder, ErpDistributionOrder.STATUS_FAIL_UNSPLIT, ErpDistributionOrder.FAIL_STEP_SPLIT, failMsg);
                        updateQueueStatus(erpGoodsOrder, "2", failQueueMsg(failMsg, erpOrder));
                        continue;
                    }

                    // 校验2：库存
                    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                        String failMsg = "下发商品库存不足";
                        markDistributionFail(erpOrder, ErpDistributionOrder.STATUS_FAIL_UNSPLIT, ErpDistributionOrder.FAIL_STEP_SPLIT, failMsg);
                        updateQueueStatus(erpGoodsOrder, "2", failQueueMsg(failMsg, erpOrder));
                        continue;
                    }

                    if (isDistribution) {
                        // 校验3：下单人余额
                        SysUser payUser = userService.selectUserOne(erpGoodsOrder.getCreatedBy());
                        BigDecimal currentBalance = (payUser == null || payUser.getBalance() == null) ? BigDecimal.ZERO : payUser.getBalance();
                        if (currentBalance.compareTo(totalPrice) < 0) {
                            String failMsg = "余额不足：应付" + totalPrice.toPlainString() + "分，当前余额" + currentBalance.toPlainString() + "分";
                            markDistributionFail(erpOrder, ErpDistributionOrder.STATUS_FAIL_UNSPLIT, ErpDistributionOrder.FAIL_STEP_SPLIT, failMsg);
                            updateQueueStatus(erpGoodsOrder, "2", failQueueMsg(failMsg, erpOrder));
                            continue;
                        }
                        Long warehouseUserId = Long.parseLong(psiProduct.get("about_id").toString());
                        writeDistLog(erpOrder, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                                ErpDistributionOrderLog.ACTION_DISPATCH_BEGIN,
                                erpOrder.getStatus(), erpOrder.getStatus(),
                                erpOrder.getOwnerId(), totalPrice.longValue(),
                                ErpDistributionOrderLog.SETTLE_FREEZING,
                                ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "开始分账冻结");
                        BigDecimal payBalanceBefore = currentBalance;
                        // 1) 分账
                        doSplitAccount(erpGoodsOrder, psiProduct, stcokData.get("warehouses"),
                                payUser, currentBalance, adminUser, warehouseUserId,
                                price, handlingFeePlatform, handlingFeeWarehouse, totalPrice,
                                psiEmployees, psiSplitAccountConfigWarehouse, psiSplitAccountConfigPlatform,
                                erpGoodsOrder.getOrderSn() + "-" + erpGoodsOrder.getShopErpId() + "-" + goodsDto.getGoodsId());
                                String payFlow = flowText("分销商余额 ", payBalanceBefore, payUser == null ? null : payUser.getBalance());
                        // 2) 分账成功
                        erpDistributionOrderService.markSplitOk(erpOrder.getId());
                        // 3) 下发 PSI 销售订单（关联 ERP 分销订单）
                        Boolean bool = createSalesOrder(erpGoodsOrder, psiProduct, goodsDto, quantity, erpOrder);
                        // 库存同步：PSI 建单成功后才减平台库存；失败会冲正分账，不动库存
                        if (bool != null && bool) {
                            synchronizeStockNew(psiProduct.get("id").toString(), warehouseUserId, -1, quantity.intValue(), erpGoodsOrder);
                        }
                        if (bool != null && bool) {
                            writeDistLog(erpOrder, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                                    ErpDistributionOrderLog.ACTION_DISPATCH_AUTO,
                                    erpOrder.getStatus(), ErpDistributionOrder.STATUS_PSI_OK,
                                    erpOrder.getOwnerId(), totalPrice.longValue(),
                                    ErpDistributionOrderLog.SETTLE_FROZEN,
                                    ErpDistributionOrderLog.OPERATOR_SYSTEM, null, joinFlowText("自动下发成功", payFlow));
                        }
                        if (!bool) {
                            boolean rolledBack = rollbackSplitForFail(erpOrder, "PSI销售订单创建失败");
                            String failMsg = "PSI销售订单创建失败" + (rolledBack ? "（分账已冲正）" : "");
                            erpDistributionOrderService.markFail(erpOrder.getId(),
                                    ErpDistributionOrder.STATUS_FAIL_UNSPLIT, ErpDistributionOrder.FAIL_STEP_PSI, failMsg);
                            writeDistLog(erpOrder, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                                    ErpDistributionOrderLog.ACTION_DISPATCH_FAIL,
                                    erpOrder.getStatus(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                                    erpOrder.getOwnerId(), totalPrice.longValue(),
                                    rolledBack ? ErpDistributionOrderLog.SETTLE_NONE : ErpDistributionOrderLog.SETTLE_FROZEN,
                                    ErpDistributionOrderLog.OPERATOR_SYSTEM, null, failMsg);
                        }
                    } else {
                        // 自营：不分账，直接创建销售订单
                        createSalesOrder(erpGoodsOrder,psiProduct,goodsDto,quantity);
                        String log = synchronizeStockNew(psiProduct.get("id").toString(),erpGoodsOrder.getCreatedBy(),-1,quantity.intValue(),erpGoodsOrder);
                    }
                }

                if (goodsCount == 0){
                    updateQueueStatus(erpGoodsOrder, "2", "失败：商品异常,销售数量为0；订单号："+erpGoodsOrder.getOrderSn());
                }
            }else{
                updateQueueStatus(erpGoodsOrder, "2", "失败："+productResMap.get("msg"));
            }
        }else {
            updateQueueStatus(erpGoodsOrder, "2", "失败：商品ID异常无法解析");
        }
    }

    @Override
    public String savePsiSyncLog(String productId, String productUserId,
                                 String erpOrderJson, String platform,
                                 String updateType, String shopCreateBy) {
        // 写日志只是辅助动作：任何异常（含 PSI/nginx 返回 HTML、字段超长）都不能中断下发
        try {
            // 分销单日志脱敏（商品所有者 != 下单人）
            String[] syncLogJson = buildDistributionSyncLog(erpOrderJson, platform, productId, toLongValue(productUserId));
            erpOrderJson = syncLogJson[0];
            platform = syncLogJson[1];
            Map synchronizationShopLog = new HashMap();
            synchronizationShopLog.put("product_id", productId);
            synchronizationShopLog.put("product_user_id", productUserId);
            // 列有长度上限，超长截断（正常单只有几百字节，避免大单写库失败）
            synchronizationShopLog.put("erp_order", truncateText(erpOrderJson, 60000));
            synchronizationShopLog.put("platform", truncateText(platform, 60000));
            synchronizationShopLog.put("update_type", updateType);
            synchronizationShopLog.put("shop_id", 1L);
            synchronizationShopLog.put("shop_name", "PSI");
            synchronizationShopLog.put("shop_type", "0");
            synchronizationShopLog.put("shop_create_by", shopCreateBy);
            String addRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/synchronization-shop-log/save", synchronizationShopLog);
            if (addRes == null || !addRes.trim().startsWith("{")) {
                log.warn("创建PSI库存同步日志返回非JSON，已忽略: {}", abbreviateText(addRes));
                return null;
            }
            Map addResMap;
            try {
                addResMap = JsonUtil.transferToObj(addRes, Map.class);
            } catch (Exception e) {
                log.warn("创建PSI库存同步日志解析失败，已忽略: {}", abbreviateText(addRes));
                return null;
            }
            if (addResMap == null || !(addResMap.get("data") instanceof Map)) {
                log.warn("创建PSI库存同步日志失败，已忽略: {}", abbreviateText(addRes));
                return null;
            }
            Map addResDataMap = (Map) addResMap.get("data");
            return addResDataMap.get("id") != null ? addResDataMap.get("id").toString() : null;
        } catch (Exception e) {
            log.warn("创建PSI库存同步日志异常（不影响下发）: {}", e.getMessage());
            return null;
        }
    }

    /** 截断超长文本，避免写库超列长度 */
    private static String truncateText(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    /** 原始响应摘要（用于日志，避免把整段 HTML 打进日志） */
    private static String abbreviateText(String text) {
        if (text == null) {
            return "null";
        }
        String one = text.replaceAll("\\s+", " ").trim();
        return one.length() > 300 ? one.substring(0, 300) + "..." : one;
    }

    @Override
    public void updatePsiSyncLog(String id, String shopCreateBy, String quantity,
                                  String inventory, String inventoryOld,
                                  String code, String msg) {
        if (id == null) return;
        Map synchronizationShopLogUpdate = new HashMap();
        synchronizationShopLogUpdate.put("shop_create_by", shopCreateBy);
        synchronizationShopLogUpdate.put("id", id);
        synchronizationShopLogUpdate.put("quantity", quantity);
        synchronizationShopLogUpdate.put("inventory", inventory);
        synchronizationShopLogUpdate.put("inventory_old", inventoryOld);
        synchronizationShopLogUpdate.put("code", code);
        synchronizationShopLogUpdate.put("msg", msg);
        String res = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/synchronization-shop-log/update", synchronizationShopLogUpdate);
        System.out.println("修改PSI库存日志：" + res);
    }

    /**
     * 简化更新队列状态
     */
    /** 安全转 BigDecimal */
    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 写分销订单操作日志（失败不影响主流程） */
    /** 金额（分）转元 */
    private static String yuanOfFen(BigDecimal fen) {
        if (fen == null) {
            return "0.00";
        }
        return fen.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 金额变动描述：余额 2961.68→2954.37(-7.31)，无变动返回 null */
    private static String flowText(String label, BigDecimal before, BigDecimal after) {
        if (before == null || after == null) {
            return null;
        }
        if (before.compareTo(after) == 0) {
            return null;
        }
        BigDecimal diff = after.subtract(before);
        return label + yuanOfFen(before) + "→" + yuanOfFen(after)
                + "(" + (diff.signum() > 0 ? "+" : "") + yuanOfFen(diff) + ")";
    }

    /** 说明文本追加资金流 */
    private static String joinFlowText(String base, String flow) {
        return (flow == null || flow.isEmpty()) ? base : base + "｜资金：" + flow;
    }

    /** 说明文本追加多段资金流 */
    private static String joinFlowTexts(String base, java.util.List<String> flows) {
        if (flows == null || flows.isEmpty()) {
            return base;
        }
        return base + "｜资金：" + String.join("；", flows);
    }

    private void writeDistLog(ErpDistributionOrder dist, int category, String action,
                              Integer fromStatus, Integer toStatus,
                              Long accountId, Long amount, Integer settleStatus,
                              int operatorType, Long operatorId, String remark) {
        if (dist == null || dist.getId() == null) {
            return;
        }
        try {
            ErpDistributionOrderLog entry = new ErpDistributionOrderLog();
            entry.setDistOrderId(dist.getId());
            entry.setCategory(category);
            entry.setAction(action);
            entry.setFromStatus(fromStatus);
            entry.setToStatus(toStatus);
            entry.setSellerId(dist.getSellerId());
            entry.setSellerName(resolveSellerName(dist.getSellerId()));
            entry.setAccountId(accountId);
            entry.setAmount(amount);
            entry.setSettleStatus(settleStatus);
            entry.setOperatorType(operatorType);
            entry.setOperatorId(operatorId);
            if (remark != null && remark.length() > 255) {
                remark = remark.substring(0, 255);
            }
            if (ErpDistributionOrderLog.ACTION_DISPATCH_AUTO.equals(action)
                    || ErpDistributionOrderLog.ACTION_DISPATCH_MANUAL.equals(action)) {
                String wh = dist.getSellerId() == null ? "" : resolveSellerName(dist.getSellerId());
                remark = (remark == null ? "" : remark) + "｜下发仓库：" + (wh.isEmpty() ? "-" : wh)
                        + "(" + (dist.getSellerId() == null ? "-" : dist.getSellerId()) + ")";
            }
            if (remark != null && remark.length() > 255) {
                remark = remark.substring(0, 255);
            }
            entry.setRemark(remark);
            entry.setCreateTime(new Date());
            erpDistributionOrderLogService.write(entry);
        } catch (Exception e) {
            log.error("写ERP分销订单日志失败 distId={} action={}", dist.getId(), action, e);
        }
    }

    /** 卖家名称：优先店铺名，其次昵称/账号 */
    private String resolveSellerName(Long sellerId) {
        if (sellerId == null) {
            return null;
        }
        try {
            return userService.selectShopName(sellerId);
        } catch (Exception e) {
            return null;
        }
    }

    /** 按来源平台订单写日志（订单完成/退款用，只写已下发过的行） */
    private void writeDistLogBySource(Long sourceOrderId, int category, String action,
                                      Integer fromStatus, Integer toStatus,
                                      Long accountId, Long amount, Integer settleStatus,
                                      int operatorType, Long operatorId, String remark) {
        try {
            List<ErpDistributionOrder> list = erpDistributionOrderService.listBySourceOrderId(sourceOrderId);
            if (list == null || list.isEmpty()) {
                return;
            }
            for (ErpDistributionOrder dist : list) {
                // 仅写已下发过的分销单；自营单不会有分销记录，不受影响
                Integer distStatus = dist.getStatus();
                if (distStatus == null
                        || distStatus == ErpDistributionOrder.STATUS_PENDING
                        || distStatus == ErpDistributionOrder.STATUS_SPLIT_OK
                        || distStatus == ErpDistributionOrder.STATUS_FAIL_UNSPLIT) {
                    continue;
                }
                writeDistLog(dist, category, action, fromStatus, toStatus,
                        accountId, amount, settleStatus, operatorType, operatorId, remark);
            }
        } catch (Exception e) {
            log.error("写ERP分销订单日志失败 sourceOrderId={} action={}", sourceOrderId, action, e);
        }
    }

    /** 分销单失败落库（自营单 erpOrder 为 null，直接跳过） */
    private void markDistributionFail(ErpDistributionOrder erpOrder, int status, int failStep, String msg) {
        if (erpOrder == null || erpOrder.getId() == null) {
            return;
        }
        try {
            erpDistributionOrderService.markFail(erpOrder.getId(), status, failStep, msg);
            writeDistLog(erpOrder, ErpDistributionOrderLog.CATEGORY_DISPATCH, ErpDistributionOrderLog.ACTION_DISPATCH_FAIL,
                    null, status, erpOrder.getOwnerId(), erpOrder.getTotalAmount(),
                    ErpDistributionOrderLog.SETTLE_NONE,
                    ErpDistributionOrderLog.OPERATOR_SYSTEM, null, msg);
        } catch (Exception e) {
            log.error("标记ERP分销订单失败 id={} msg={}", erpOrder.getId(), msg, e);
        }
    }

    /** 队列失败消息：分销单带上 ERP 单号，指引人工处理 */
    private static String failQueueMsg(String reason, ErpDistributionOrder erpOrder) {
        if (erpOrder == null) {
            return "失败：" + reason;
        }
        return "失败：" + reason + "（已创建ERP分销订单" + erpOrder.getOrderNo() + "，请到分销订单页手动下发）";
    }

    private void updateQueueStatus(ErpGoodsOrder erpGoodsOrder, String status, String msg) {
        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
        erpGoodsOrderQueue.setStatus(status);
        erpGoodsOrderQueue.setMsg(msg);
        erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
    }

    /**
     * 获取运费
     * @return
     */
    /**
     * 分销订单手动下发：按 ISBN/书名 查询可选商品（跨租户）
     */
    public Map listDistributionProducts(Long distOrderId, String keyword) {
        Map result = new HashMap();
        try {
            if (distOrderId == null) {
                result.put("code", "400");
                result.put("msg", "分销订单ID不能为空");
                return result;
            }
            ErpDistributionOrder dist = erpDistributionOrderService.getById(distOrderId);
            if (dist == null) {
                result.put("code", "400");
                result.put("msg", "分销订单不存在");
                return result;
            }
            ErpGoodsOrder order = erpGoodsOrderMapper.selectById(dist.getSourceOrderId());
            if (order == null) {
                result.put("code", "400");
                result.put("msg", "平台订单不存在");
                return result;
            }
            if (StringUtils.isEmpty(keyword)) {
                result.put("code", "400");
                result.put("msg", "请输入ISBN或书名");
                return result;
            }

            // 该单售后完成过的仓库（永久排除，多轮累加）：不允许再被重新选中下发
            java.util.Set<String> refundedOwners = new java.util.HashSet();
            try {
                for (Long sid : afterSalesService.listRefundedSellerIds(dist.getId())) {
                    if (sid != null) {
                        refundedOwners.add(String.valueOf(sid));
                    }
                }
            } catch (Exception ignore) {
            }
            boolean filteredByAfterSales = false;

            String kw = java.net.URLEncoder.encode(keyword.trim(), "UTF-8");
            String productRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                    "/api/product_book/list?page=1&page_size=50&keyword=" + kw);
            Map productResMap = JsonUtil.transferToObj(productRes, Map.class);
            if (productResMap == null || !"200".equals(String.valueOf(productResMap.get("code")))) {
                result.put("code", "400");
                result.put("msg", "查询商品失败: " + (productResMap == null ? "无响应" : productResMap.get("msg")));
                return result;
            }
            Map dataMap = (Map) productResMap.get("data");
            List goodsList = dataMap == null ? null : (List) dataMap.get("list");
            if (goodsList == null || goodsList.isEmpty()) {
                result.put("code", "400");
                result.put("msg", "未查询到相关商品");
                return result;
            }

            // 下单人（分润方）手续费配置
            BigDecimal ratioPlatform = new BigDecimal("0.03");
            BigDecimal addPlatform = new BigDecimal("0.1").multiply(new BigDecimal(100));
            try {
                SysUser orderUser = userService.selectUserOne(dist.getOwnerId());
                PsiEmployees emp = orderUser == null ? null
                        : psiEmployeesService.selectOneByAboutIdAndPhone(dist.getOwnerId(), orderUser.getPhonenumber());
                if (emp != null && emp.getRuleValue() != null) {
                    List ruleList = JsonUtil.transferToObj(emp.getRuleValue(), List.class);
                    for (Object rv : ruleList) {
                        Map rvMap = (Map) rv;
                        if ("分润方".equals(String.valueOf(rvMap.get("product_type")))) {
                            ratioPlatform = new BigDecimal(String.valueOf(rvMap.get("ratio")));
                            addPlatform = new BigDecimal(String.valueOf(rvMap.get("add_amount"))).multiply(new BigDecimal(100));
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            // 平台订单快照里的商品图，作为最终兜底（product_book / product 都没图时）
            Object orderImgs = null;
            try {
                Map orderSnapshot = JsonUtil.transferToObj(dist.getItemList(), Map.class);
                if (orderSnapshot != null) {
                    orderImgs = orderSnapshot.get("goodsImgs");
                }
            } catch (Exception ignore) {
            }

            List enrichedList = new ArrayList();
            // product_book 存在重复行（同一 self_id 多条），按 货主+商品ID 去重
            java.util.Set seenProducts = new java.util.HashSet();
            for (Object obj : goodsList) {
                Map item = (Map) obj;
                String aboutId = String.valueOf(item.get("about_id"));
                String pid = String.valueOf(item.get("self_id"));
                if (!seenProducts.add(aboutId + ":" + pid)) {
                    continue;
                }
                if (!refundedOwners.isEmpty() && refundedOwners.contains(aboutId)) {
                    filteredByAfterSales = true;
                    continue;
                }

                int quantity = 0;
                BigDecimal cost = BigDecimal.ZERO;
                Object warehouseName = item.get("warehouse_name");
                Object locationName = item.get("location_name");
                String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                        "/api/product/getProductInventory?user_id=" + aboutId + "&product_id=" + pid + "&type=1");
                Map stockResMap = JsonUtil.transferToObj(stockRes, Map.class);
                // 商品已删除/不存在（full_info 与库存接口都会返回非200），不进可选列表
                if (stockResMap == null || !"200".equals(String.valueOf(stockResMap.get("code")))) {
                    continue;
                }
                if ("200".equals(String.valueOf(stockResMap.get("code")))) {
                    Map stockData = (Map) stockResMap.get("data");
                    if (stockData != null) {
                        Object qtyObj = stockData.get("product_quantity") != null ? stockData.get("product_quantity") : stockData.get("quantity");
                        if (qtyObj != null) {
                            quantity = Integer.parseInt(String.valueOf(qtyObj));
                        }
                        // 仓库/货位以库存接口为准（product_book 里的仓库字段可能是旧快照或为空）
                        List whList = (List) stockData.get("warehouses");
                        if (whList != null && !whList.isEmpty()) {
                            Map wh = (Map) whList.get(0);
                            Object wName = wh.get("warehouse_name");
                            Object lName = wh.get("location_name");
                            if (wName != null && !String.valueOf(wName).trim().isEmpty()) {
                                warehouseName = wName;
                            }
                            if (lName != null && !String.valueOf(lName).trim().isEmpty()) {
                                locationName = lName;
                            }
                        }
                    }
                    try {
                        cost = getCost(order, stockRes);
                    } catch (Exception ignore) {
                    }
                }

                // product_book 若为旧快照（书名/图片/价格缺失），回查租户商品主档
                Object bookName = item.get("book_name");
                Object salePriceObj = item.get("sale_price");
                Object liveImage = item.get("live_image");
                String imgStr = liveImage == null ? "" : liveImage.toString().trim();
                boolean imgEmpty = imgStr.isEmpty() || "[]".equals(imgStr);
                boolean nameEmpty = bookName == null || String.valueOf(bookName).trim().isEmpty();
                boolean priceEmpty = salePriceObj == null || "0".equals(String.valueOf(salePriceObj));
                if (imgEmpty || nameEmpty || priceEmpty) {
                    try {
                        String fullInfoRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                                "/api/product/full_info?user_id=" + aboutId + "&product_id=" + pid);
                        Map fullInfoMap = JsonUtil.transferToObj(fullInfoRes, Map.class);
                        if (fullInfoMap != null && "200".equals(String.valueOf(fullInfoMap.get("code")))) {
                            Map productMap = (Map) fullInfoMap.get("data");
                            if (productMap != null) {
                                if (nameEmpty && productMap.get("name") != null) {
                                    bookName = productMap.get("name");
                                }
                                if (imgEmpty && productMap.get("live_image") != null) {
                                    liveImage = productMap.get("live_image");
                                }
                                if (priceEmpty && productMap.get("sale_price") != null) {
                                    salePriceObj = productMap.get("sale_price");
                                }
                            }
                        }
                    } catch (Exception ignore) {
                    }
                }

                String finalImgStr = liveImage == null ? "" : liveImage.toString().trim();
                if ((finalImgStr.isEmpty() || "[]".equals(finalImgStr)) && orderImgs != null) {
                    liveImage = orderImgs;
                }

                BigDecimal salePrice = toDecimal(salePriceObj);
                BigDecimal fee = BigDecimal.ZERO;
                if (!aboutId.equals(String.valueOf(dist.getOwnerId()))) {
                    fee = salePrice.add(cost).multiply(ratioPlatform)
                            .setScale(0, RoundingMode.CEILING).add(addPlatform);
                }

                Map enriched = new HashMap();
                enriched.put("product_id", item.get("self_id"));
                enriched.put("barcode", item.get("barcode"));
                enriched.put("book_name", bookName);
                enriched.put("sale_price", salePriceObj);
                enriched.put("about_id", aboutId);
                enriched.put("freight", cost);
                enriched.put("handling_fee", fee);
                enriched.put("quantity", quantity);
                enriched.put("live_image", liveImage);
                enriched.put("warehouse_name", warehouseName);
                enriched.put("location_name", locationName);
                enrichedList.add(enriched);
            }

            if (enrichedList.isEmpty() && filteredByAfterSales) {
                result.put("code", "400");
                result.put("msg", "暂无商品可下发");
                return result;
            }
            result.put("code", "200");
            result.put("data", enrichedList);
        } catch (Exception e) {
            log.error("查询分销订单可选商品失败 id={} keyword={}", distOrderId, keyword, e);
            result.put("code", "500");
            result.put("msg", "系统异常: " + e.getMessage());
        }
        return result;
    }

    /**
     * 分销订单手动下发（选商品 → 重新算钱 → 分账 → 下发 PSI），复用当前 ERP 分销订单
     */
    public Map dispatchDistributionOrder(Long distOrderId, Long productId, Long aboutId, Long operatorId) {
        Map result = new HashMap();
        ErpDistributionOrder dist = null;
        boolean splitDone = false;
        try {
            if (distOrderId == null || productId == null || aboutId == null) {
                result.put("code", "400");
                result.put("msg", "参数不完整");
                return result;
            }
            dist = erpDistributionOrderService.getById(distOrderId);
            if (dist == null) {
                result.put("code", "400");
                result.put("msg", "分销订单不存在");
                return result;
            }
            int status = dist.getStatus() == null ? ErpDistributionOrder.STATUS_PENDING : dist.getStatus();
            if (status != ErpDistributionOrder.STATUS_PENDING && status != ErpDistributionOrder.STATUS_FAIL_UNSPLIT) {
                result.put("code", "400");
                result.put("msg", "该订单已下发，不支持更换商品");
                return result;
            }
            // 售后完成过的仓库，永久不允许再次下发（防绕过前端直接调接口）
            try {
                java.util.List<Long> refundedSellers = afterSalesService.listRefundedSellerIds(dist.getId());
                if (refundedSellers != null && refundedSellers.contains(aboutId)) {
                    result.put("code", "400");
                    result.put("msg", "该仓库在本订单已售后过，不能再次下发，请选择其他仓库");
                    return result;
                }
            } catch (Exception ignore) {
            }

            ErpGoodsOrder order = erpGoodsOrderMapper.selectById(dist.getSourceOrderId());
            if (order == null) {
                result.put("code", "400");
                result.put("msg", "平台订单不存在");
                return result;
            }
            erpDistributionOrderService.incrementRetry(dist.getId());

            // 1. 商品
            String fullInfoRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                    "/api/product/full_info?user_id=" + aboutId + "&product_id=" + productId);
            Map fullInfoMap = JsonUtil.transferToObj(fullInfoRes, Map.class);
            if (fullInfoMap == null || !"200".equals(String.valueOf(fullInfoMap.get("code")))) {
                failDistributionDispatch(dist, "查询商品信息失败");
                result.put("code", "400");
                result.put("msg", "查询商品信息失败");
                return result;
            }
            Map psiProduct = (Map) fullInfoMap.get("data");
            psiProduct.put("about_id", String.valueOf(aboutId));

            // 2. 库存 + 运费
            String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                    "/api/product/getProductInventory?user_id=" + aboutId + "&product_id=" + productId + "&type=1");
            Map stockResMap = JsonUtil.transferToObj(stockRes, Map.class);
            Map stockData = stockResMap == null ? null : (Map) stockResMap.get("data");
            int quantity = 0;
            if (stockData != null) {
                // 优先用选中商品自身的可用库存（同 ISBN+品相 的聚合量会虚高，导致建单时才报库存不足）
                Object qtyObj = stockData.get("product_quantity") != null ? stockData.get("product_quantity") : stockData.get("quantity");
                if (qtyObj != null) {
                    quantity = Integer.parseInt(String.valueOf(qtyObj));
                }
            }
            if (quantity <= 0) {
                failDistributionDispatch(dist, "库存不足");
                result.put("code", "400");
                result.put("msg", "库存不足");
                return result;
            }
            BigDecimal cost = BigDecimal.ZERO;
            try {
                cost = getCost(order, stockRes);
            } catch (Exception e) {
                System.out.println("[分销手动下发] 运费解析失败 orderSn=" + order.getOrderSn() + " err=" + e.getMessage());
            }

            // 3. 金额
            BigDecimal salePrice = toDecimal(psiProduct.get("sale_price"));
            BigDecimal price = salePrice.add(cost);

            SysUser payUser = userService.selectUserOne(dist.getOwnerId());
            BigDecimal balance = (payUser == null || payUser.getBalance() == null) ? BigDecimal.ZERO : payUser.getBalance();

            PsiEmployees psiEmployees = null;
            try {
                if (payUser != null) {
                    psiEmployees = psiEmployeesService.selectOneByAboutIdAndPhone(dist.getOwnerId(), payUser.getPhonenumber());
                }
            } catch (Exception ignore) {
            }
            PsiSplitAccountConfig cfgWarehouse = new PsiSplitAccountConfig();
            PsiSplitAccountConfig cfgPlatform = new PsiSplitAccountConfig();
            if (psiEmployees != null && psiEmployees.getRuleValue() != null) {
                List ruleList = JsonUtil.transferToObj(psiEmployees.getRuleValue(), List.class);
                for (Object rv : ruleList) {
                    Map rvMap = (Map) rv;
                    if ("仓库方".equals(String.valueOf(rvMap.get("product_type")))) {
                        cfgWarehouse.setProductType("仓库方");
                        cfgWarehouse.setRatio(new BigDecimal(String.valueOf(rvMap.get("ratio"))));
                        cfgWarehouse.setAddAmount(new BigDecimal(String.valueOf(rvMap.get("add_amount"))).multiply(new BigDecimal(100)));
                    } else if ("分润方".equals(String.valueOf(rvMap.get("product_type")))) {
                        cfgPlatform.setProductType("分润方");
                        cfgPlatform.setRatio(new BigDecimal(String.valueOf(rvMap.get("ratio"))));
                        cfgPlatform.setAddAmount(new BigDecimal(String.valueOf(rvMap.get("add_amount"))).multiply(new BigDecimal(100)));
                    }
                }
            } else {
                psiEmployees = new PsiEmployees();
                psiEmployees.setSplitAccountConfigId(1L);
                psiEmployees.setRuleName("默认规则");
                cfgWarehouse.setProductType("仓库方");
                cfgWarehouse.setRatio(new BigDecimal("0.03"));
                cfgWarehouse.setAddAmount(new BigDecimal("0.1").multiply(new BigDecimal(100)));
                cfgPlatform.setProductType("分润方");
                cfgPlatform.setRatio(new BigDecimal("0.03"));
                cfgPlatform.setAddAmount(new BigDecimal("0.1").multiply(new BigDecimal(100)));
            }

            BigDecimal feePlatform = price.multiply(cfgPlatform.getRatio()).setScale(0, RoundingMode.CEILING).add(cfgPlatform.getAddAmount());
            BigDecimal feeWarehouse = price.multiply(cfgWarehouse.getRatio()).setScale(0, RoundingMode.CEILING).add(cfgWarehouse.getAddAmount());
            BigDecimal totalPrice = price.add(feePlatform);

            boolean isSelf = String.valueOf(dist.getOwnerId()).equals(String.valueOf(aboutId));
            if (!isSelf && balance.compareTo(totalPrice) < 0) {
                String msg = "余额不足：应付" + totalPrice.toPlainString() + "分，当前余额" + balance.toPlainString() + "分";
                failDistributionDispatch(dist, msg);
                result.put("code", "400");
                result.put("msg", msg);
                return result;
            }

            // 4. 更新 ERP 单的商品与金额（换商品后重新计算）
            SysUser seller = userService.selectUserOne(aboutId);
            dist.setSellerId(aboutId);
            dist.setSellerPhone(seller == null ? null : seller.getPhonenumber());
            dist.setProductId(psiProduct.get("id") == null ? productId : Long.parseLong(psiProduct.get("id").toString()));
            dist.setBarcode(psiProduct.get("barcode") == null ? null : psiProduct.get("barcode").toString());
            dist.setProductName(psiProduct.get("name") == null ? null : psiProduct.get("name").toString());
            dist.setAppearance(psiProduct.get("appearance") == null ? null : psiProduct.get("appearance").toString());
            dist.setUnitPrice(salePrice.longValue());
            dist.setFreight(cost.longValue());
            dist.setBookPrice(price.longValue());
            dist.setPlatformFee(feePlatform.longValue());
            dist.setWarehouseFee(feeWarehouse.longValue());
            dist.setTotalAmount(totalPrice.longValue());
            dist.setSplitConfigId(psiEmployees == null ? null : psiEmployees.getSplitAccountConfigId());
            dist.setSplitConfigName(psiEmployees == null ? null : psiEmployees.getRuleName());
            Map ruleMap = new LinkedHashMap();
            ruleMap.put("warehouse", cfgWarehouse);
            ruleMap.put("platform", cfgPlatform);
            dist.setSplitRule(JsonUtil.transferToJson(ruleMap));
            dist.setUpdateBy(operatorId);
            dist.setUpdateTime(new Date());
            erpDistributionOrderService.updateDispatchInfo(dist);

            // 5. 分账
            GoodsDto goodsDto = JsonUtil.transferToObj(order.getItemList(), GoodsDto.class);
            if (goodsDto == null) {
                goodsDto = new GoodsDto();
            }
            BigDecimal payBalanceBefore = balance;
            String payFlow = null;
            if (!isSelf) {
                writeDistLog(dist, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                        ErpDistributionOrderLog.ACTION_DISPATCH_BEGIN,
                        status, status, dist.getOwnerId(), totalPrice.longValue(),
                        ErpDistributionOrderLog.SETTLE_FREEZING,
                        ErpDistributionOrderLog.OPERATOR_ADMIN, operatorId, "手动下发开始分账冻结");
                doSplitAccount(order, psiProduct, stockData.get("warehouses"),
                        payUser, balance, userService.selectUserOne(1L), aboutId,
                        price, feePlatform, feeWarehouse, totalPrice,
                        psiEmployees, cfgWarehouse, cfgPlatform,
                        order.getOrderSn() + "-" + order.getShopErpId() + "-" + goodsDto.getGoodsId());
                        payFlow = flowText("分销商余额 ", payBalanceBefore, payUser == null ? null : payUser.getBalance());
                splitDone = true;
            }
            erpDistributionOrderService.markSplitOk(dist.getId());

            // 6. 下发 PSI
            Boolean ok = createSalesOrder(order, psiProduct, goodsDto, BigDecimal.ONE, dist);
            if (ok == null || !ok) {
                boolean rolledBack = rollbackSplitForFail(dist, "PSI销售订单创建失败");
                String failMsg = "PSI销售订单创建失败" + (rolledBack ? "（分账已冲正）" : "");
                erpDistributionOrderService.markFail(dist.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                        ErpDistributionOrder.FAIL_STEP_PSI, failMsg);
                writeDistLog(dist, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                        ErpDistributionOrderLog.ACTION_DISPATCH_FAIL,
                        status, ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                        dist.getOwnerId(), dist.getTotalAmount(),
                        rolledBack ? ErpDistributionOrderLog.SETTLE_NONE : ErpDistributionOrderLog.SETTLE_FROZEN,
                        ErpDistributionOrderLog.OPERATOR_SYSTEM, null, failMsg);
                result.put("code", "500");
                result.put("msg", failMsg);
                return result;
            }

            // 7. 同步店铺库存
            try {
                synchronizeStockNew(psiProduct.get("id").toString(), aboutId, -1, 1, order);
            } catch (Exception e) {
                log.error("分销手动下发-库存同步失败 distId={}", dist.getId(), e);
            }

            writeDistLog(dist, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                    ErpDistributionOrderLog.ACTION_DISPATCH_MANUAL,
                    status, ErpDistributionOrder.STATUS_PSI_OK,
                    dist.getOwnerId(), totalPrice.longValue(),
                    isSelf ? ErpDistributionOrderLog.SETTLE_NONE : ErpDistributionOrderLog.SETTLE_FROZEN,
                    (operatorId != null && operatorId == 1L)
                            ? ErpDistributionOrderLog.OPERATOR_ADMIN : ErpDistributionOrderLog.OPERATOR_BUYER,
                    operatorId, joinFlowText("手动下发成功", payFlow));
            // 手动下发成功：把该订单的下发队列标记为成功，保证「订单完成/退款」事件能被正常识别
            markQueuePushed(order.getId(), "手动下发成功（分销）");

            result.put("code", "200");
            result.put("msg", "手动下发成功");
            return result;
        } catch (Exception e) {
            log.error("分销订单手动下发失败 id={}", distOrderId, e);
            writeDistLog(dist, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                    ErpDistributionOrderLog.ACTION_DISPATCH_FAIL,
                    null, splitDone ? ErpDistributionOrder.STATUS_FAIL_SPLIT
                            : ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                    dist == null ? null : dist.getOwnerId(),
                    dist == null ? null : dist.getTotalAmount(),
                    splitDone ? ErpDistributionOrderLog.SETTLE_FROZEN : ErpDistributionOrderLog.SETTLE_NONE,
                    ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "手动下发异常: " + e.getMessage());
            if (dist != null && dist.getId() != null) {
                try {
                    if (splitDone) {
                        boolean rolledBack = rollbackSplitForFail(dist, "手动下发异常: " + e.getMessage());
                        erpDistributionOrderService.markFail(dist.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                                ErpDistributionOrder.FAIL_STEP_PSI,
                                "手动下发异常: " + e.getMessage() + (rolledBack ? "（分账已冲正）" : ""));
                    } else {
                        erpDistributionOrderService.markFail(dist.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                                ErpDistributionOrder.FAIL_STEP_SPLIT, "手动下发异常: " + e.getMessage());
                    }
                } catch (Exception ignore) {
                }
            }
            result.put("code", "500");
            result.put("msg", "手动下发异常: " + e.getMessage());
            return result;
        }
    }

    /** 手动下发成功后，同步把该订单的下发队列（order_type=0）置为成功 */
    private void markQueuePushed(Long orderId, String msg) {
        if (orderId == null) {
            return;
        }
        try {
            ErpGoodsOrderQueue probe = new ErpGoodsOrderQueue();
            probe.setErpGoodsOrderId(orderId);
            probe.setOrderType("0");
            List<ErpGoodsOrderQueue> list = erpGoodsOrderQueueService.getList(probe);
            if (list != null && !list.isEmpty()) {
                for (ErpGoodsOrderQueue q : list) {
                    ErpGoodsOrderQueue upd = new ErpGoodsOrderQueue();
                    upd.setId(q.getId());
                    upd.setStatus("1");
                    upd.setMsg(msg);
                    erpGoodsOrderQueueService.update(upd);
                }
            } else {
                ErpGoodsOrderQueue q = new ErpGoodsOrderQueue();
                q.setErpGoodsOrderId(orderId);
                q.setStatus("1");
                q.setOrderType("0");
                erpGoodsOrderQueueService.save(q);
                if (q.getId() != null) {
                    ErpGoodsOrderQueue upd = new ErpGoodsOrderQueue();
                    upd.setId(q.getId());
                    upd.setMsg(msg);
                    erpGoodsOrderQueueService.update(upd);
                }
            }
        } catch (Exception e) {
            log.error("标记下发队列成功失败 orderId={}", orderId, e);
        }
    }

    /** 手动下发前置校验失败：更新 ERP 单为下发失败并记录原因 */
    private void failDistributionDispatch(ErpDistributionOrder dist, String msg) {
        if (dist == null || dist.getId() == null) {
            return;
        }
        try {
            erpDistributionOrderService.markFail(dist.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                    ErpDistributionOrder.FAIL_STEP_SPLIT, msg);
            writeDistLog(dist, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                    ErpDistributionOrderLog.ACTION_DISPATCH_FAIL,
                    null, ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                    dist.getOwnerId(), dist.getTotalAmount(),
                    ErpDistributionOrderLog.SETTLE_NONE,
                    ErpDistributionOrderLog.OPERATOR_SYSTEM, null, msg);
        } catch (Exception e) {
            log.error("更新ERP分销订单失败 id={} msg={}", dist.getId(), msg, e);
        }
    }

    public BigDecimal getCost(ErpGoodsOrder erpGoodsOrder,String stockRes){
        try{
            Map stockResMap = JsonUtil.transferToObj(stockRes,Map.class);
            Map stcokData = (Map) stockResMap.get("data");
            List warehousesList = (List) stcokData.get("warehouses");
            Map warehouses = (Map) warehousesList.get(0);
            Map logistics = (Map) warehouses.get("logistics");
            Map shippingRange = JsonUtil.transferToObj(logistics.get("shippingRange").toString(),Map.class);
            String province = erpGoodsOrder.getProvince().replace("省","").replace("市","").replace("自治区","");
            for(Object key : shippingRange.keySet()){
                if(key.toString().contains(province)){
                    //获取省份的配置
                    List shippingCostList = (List) shippingRange.get(key);
                    //获取首费
                    BigDecimal headCost = new BigDecimal(shippingCostList.get(1).toString()).multiply(new BigDecimal(100));
                    return headCost;
                }
            }
            return BigDecimal.ZERO;
        }catch (Exception e){
            return BigDecimal.ZERO;
        }
    }


    /**
     * 推送订单
     * @param erpGoodsOrder
     * @param psiProduct
     * @param goodsDto
     * @return
     */
    public Boolean createSalesOrder(ErpGoodsOrder erpGoodsOrder, Map psiProduct, GoodsDto goodsDto, BigDecimal quantity){
        return createSalesOrder(erpGoodsOrder, psiProduct, goodsDto, quantity, null);
    }

    /**
     * 店铺名脱敏：保留首尾各一个字，中间用 *** 代替。
     */
    private static String maskShopName(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim();
        if (n.isEmpty()) {
            return "";
        }
        if (n.length() <= 2) {
            return n.substring(0, 1) + "***";
        }
        return n.substring(0, 1) + "***" + n.substring(n.length() - 1);
    }


    /**
     * 库存同步日志脱敏：分销单（商品所有者 != 下单人）时
     * 订单号用 ERP 分销单号、店铺显示“ERP分销店铺”、订单金额用仓库价（不含运费），
     * 同时清空平台金额字段，避免仓库方看到真实平台订单信息。
     *
     * @return [erp_order, platform]，自营单原样返回
     */
    private String[] buildDistributionSyncLog(String erpOrderJson, String platform, String productId, Long ownerId) {
        String[] result = new String[]{erpOrderJson, platform};
        if (erpOrderJson == null || ownerId == null) {
            return result;
        }
        try {
            Map orderMap = JsonUtil.transferToObj(erpOrderJson, Map.class);
            if (orderMap == null) {
                return result;
            }
            Long createdBy = toLongValue(orderMap.get("createdBy"));
            if (createdBy == null || createdBy.equals(ownerId)) {
                // 自营单，保持原样
                return result;
            }
            ErpDistributionOrder dist = findDistributionOrder(toLongValue(orderMap.get("id")), productId);
            // 平台金额、平台店铺标识统一清空（JsonUtil 序列化时会忽略 null）
            orderMap.put("payAmount", null);
            orderMap.put("goodsAmount", null);
            orderMap.put("orderChangeAmount", null);
            orderMap.put("shopId", null);
            orderMap.put("shopErpId", null);
            orderMap.put("id", null);
            if (dist != null) {
                orderMap.put("orderSn", dist.getOrderNo());
                orderMap.put("shopErpName", "ERP分销店铺");
                orderMap.put("orderTotal", dist.getUnitPrice());
            } else {
                // 历史分销单没有 ERP 分销订单：店铺名脱敏、金额置空
                Object shopName = orderMap.get("shopErpName");
                orderMap.put("shopErpName", shopName == null ? "" : maskShopName(shopName.toString()));
                orderMap.put("orderTotal", null);
            }
            Object itemListObj = orderMap.get("itemList");
            if (itemListObj instanceof String) {
                orderMap.put("itemList", maskPlatformGoodsPrice((String) itemListObj));
            }
            result[0] = JsonUtil.transferToJson(orderMap);
            result[1] = maskPlatformGoodsPrice(platform);
            return result;
        } catch (Exception e) {
            log.error("库存同步日志脱敏失败, productId={}", productId, e);
            return result;
        }
    }

    /**
     * 商品信息 JSON 脱敏：清空平台成交价 goodsPrice
     */
    private String maskPlatformGoodsPrice(String platformJson) {
        if (platformJson == null || platformJson.trim().isEmpty()) {
            return platformJson;
        }
        try {
            Map platformMap = JsonUtil.transferToObj(platformJson, Map.class);
            if (platformMap == null) {
                return platformJson;
            }
            platformMap.put("goodsPrice", null);
            return JsonUtil.transferToJson(platformMap);
        } catch (Exception e) {
            log.error("商品信息脱敏失败", e);
            return platformJson;
        }
    }

    /**
     * 按平台订单ID + 商品ID 查询 ERP 分销订单
     */
    private ErpDistributionOrder findDistributionOrder(Long sourceOrderId, String productId) {
        if (sourceOrderId == null) {
            return null;
        }
        try {
            List<ErpDistributionOrder> list = erpDistributionOrderService.listBySourceOrderId(sourceOrderId);
            if (list == null || list.isEmpty() || productId == null) {
                return null;
            }
            for (ErpDistributionOrder d : list) {
                if (d.getProductId() != null && productId.equals(d.getProductId().toString())) {
                    return d;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("查询ERP分销订单失败, sourceOrderId={}", sourceOrderId, e);
            return null;
        }
    }

    /**
     * Object 转 Long（兼容字符串/数字）
     */
    private Long toLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return new BigDecimal(value.toString().trim()).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 推送订单（支持关联 ERP 分销订单）
     */
    public Boolean createSalesOrder(ErpGoodsOrder erpGoodsOrder, Map psiProduct, GoodsDto goodsDto, BigDecimal quantity, ErpDistributionOrder erpOrder){
        // 创建PSI库存同步日志
        String logId = savePsiSyncLog(
                psiProduct.get("id").toString(),
                psiProduct.get("about_id").toString(),
                JsonUtil.transferToJson(erpGoodsOrder),
                erpGoodsOrder.getItemList(),
                "扣减库存",
                psiProduct.get("about_id").toString()
        );

        Map<String, String> requestParams = new HashMap<>();
        // 添加签名相关参数
        requestParams.put("app_key", "psi");
        requestParams.put("client_id", "psi");
        requestParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        requestParams.put("sign_method", "md5");
        // 添加业务参数（从对象中获取）
        // 关联订单id：分销走 ERP 分销订单，自营仍用平台订单
        requestParams.put("association_order_id", erpOrder != null ? erpOrder.getId().toString() : erpGoodsOrder.getId().toString());
        // 关联订单编号
        requestParams.put("association_order_no", erpOrder != null ? erpOrder.getOrderNo() : erpGoodsOrder.getOrderSn());
        // 来源类型 0-预留 1-erp订单
        requestParams.put("from_type", "1");
        // 订单日期
        Long createdAt = erpGoodsOrder.getCreatedAt();
        if (createdAt != null) {
            // 判断是13位毫秒级还是10位秒级
            if (createdAt.toString().length() == 13) {
                createdAt = createdAt / 1000; // 毫秒转秒
            }
            // 如果是10位，保持不变
            requestParams.put("order_date", createdAt.toString());
        }
        // 商品id
        requestParams.put("items[0][product_id]",psiProduct.get("id").toString());
        // 单价：统一取本行商品价（单位：分）。
        // 注意：孔网(shop_type=2) 抓单时会把整单金额复制到该订单拆出来的每一行 erp_goods_order，
        // 旧逻辑这里直接拿 orderTotal 当单价，导致"一单 N 件"在 PSI 里被记成 N 份整单金额
        // （实测一单 48 本 → 48 张销售单 × 整单额，销售额虚增 48 倍）。
        // 商品价缺失时才退回整单金额，并打日志便于排查。
        String unitPriceStr = goodsDto.getGoodsPrice();
        long unitPriceVal = 0L;
        if (unitPriceStr != null && !unitPriceStr.trim().isEmpty()) {
            try {
                unitPriceVal = new BigDecimal(unitPriceStr.trim()).longValue();
            } catch (Exception e) {
                System.out.println("[下发PSI] 商品价解析失败 goodsPrice=" + unitPriceStr
                        + " 订单号=" + erpGoodsOrder.getOrderSn());
            }
        }
        if (unitPriceVal <= 0) {
            Long orderTotalVal = erpGoodsOrder.getOrderTotal();
            unitPriceVal = orderTotalVal == null ? 0L : orderTotalVal.longValue();
            System.out.println("[下发PSI] 商品价为空，退回整单金额 orderSn=" + erpGoodsOrder.getOrderSn()
                    + " orderTotal=" + unitPriceVal);
        }
        // 分销订单：仓库方看到的是货主自己的商品价格（运费在分账日志里单独体现），
        // 不暴露买家实付金额；自营订单保持原口径。
        if (erpOrder != null) {
            Object whSalePriceObj = psiProduct.get("sale_price");
            long whSalePrice = 0L;
            if (whSalePriceObj != null) {
                try {
                    whSalePrice = new BigDecimal(whSalePriceObj.toString())
                            .setScale(0, RoundingMode.HALF_UP).longValue();
                } catch (Exception e) {
                    System.out.println("[下发PSI] 仓库商品价解析失败 sale_price=" + whSalePriceObj
                            + " 订单号=" + erpGoodsOrder.getOrderSn());
                }
            }
            if (whSalePrice <= 0) {
                whSalePrice = unitPriceVal;
            }
            requestParams.put("items[0][unit_price]", String.valueOf(whSalePrice));
        } else {
            requestParams.put("items[0][unit_price]", String.valueOf(unitPriceVal));
        }
        // 数量
        requestParams.put("items[0][quantity]","1");
        // 店铺名
        // 分销订单对仓库方脱敏：店铺名称只保留首尾各一个字
        String shopNameForPsi = erpOrder != null
                ? maskShopName(erpGoodsOrder.getShopErpName())
                : erpGoodsOrder.getShopErpName();
        requestParams.put("sales_person", shopNameForPsi);
        // 店铺id
        requestParams.put("sales_person_id", erpGoodsOrder.getShopErpId().toString());
        // 店铺创建id
        requestParams.put("about_id", psiProduct.get("about_id").toString());
        // 店铺类型
        requestParams.put("shop_type",erpGoodsOrder.getShopType().toString());
        // 分销标记：PSI 侧据此把「平台」展示成「分销」，不暴露真实平台
        if (erpOrder != null) {
            requestParams.put("is_distribution", "1");
        }
        // 收货人姓名
        requestParams.put("receiver_name", MaskUtils.maskName(erpGoodsOrder.getReceiverName()));
        // 收货人电话
        requestParams.put("receiver_phone",MaskUtils.maskPhone(erpGoodsOrder.getMobile()));
        // 收货地址
        requestParams.put("receiver_address",erpGoodsOrder.getProvince()+"-"+erpGoodsOrder.getCity()+"-"+erpGoodsOrder.getCountry()+"-"+MaskUtils.maskTown(erpGoodsOrder.getTown()));

        // 调用远程接口
        String result = InterfaceUtils.postFormWithSign(
                UrlUtil.getNewWarehouse()+"/api/sales-order/create",
                null,
                requestParams,
                "",
                "md5"
        );
        Map resultMap = null;
        try {
            resultMap = JsonUtil.transferToObj(result, Map.class);
        } catch (Exception e) {
            log.error("PSI创建销售订单返回非JSON: {}", abbreviateText(result));
        }
        if (resultMap == null) {
            // 非 JSON（如 nginx/WAF 的 HTML 页面）统一按失败处理，交给上层冲正分账
            resultMap = new HashMap();
            resultMap.put("code", "500");
            resultMap.put("msg", "PSI返回非JSON：" + abbreviateText(result));
        }
        ErpGoodsOrderQueue erpGoodsOrderQueue = null;
        if (erpGoodsOrder.getQueueId() != null && !erpGoodsOrder.getQueueId().isEmpty()) {
            erpGoodsOrderQueue = new ErpGoodsOrderQueue();
            erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
        }

        Boolean bool;
        if (resultMap.get("code") != null && "200".equals(resultMap.get("code").toString())){
            if (erpGoodsOrderQueue != null) {
                erpGoodsOrderQueue.setStatus("1");
                erpGoodsOrderQueue.setMsg("创建销售订单成功");
                erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
            }
            updatePsiSyncLog(logId, psiProduct.get("about_id").toString(), "1",
                    quantity.subtract(BigDecimal.ONE).toString(), quantity.toString(),
                    "200", "创建销售订单成功");
            // 回写 ERP 分销订单的 PSI 订单号
            if (erpOrder != null) {
                Long psiOrderId = null;
                String psiOrderNo = null;
                Object dataObj = resultMap.get("data");
                if (dataObj instanceof Map) {
                    Map dataMap = (Map) dataObj;
                    Object idObj = dataMap.get("id") != null ? dataMap.get("id") : dataMap.get("order_id");
                    if (idObj != null) {
                        try {
                            psiOrderId = Long.parseLong(idObj.toString());
                        } catch (Exception ignore) {
                        }
                    }
                    Object soNoObj = dataMap.get("so_no") != null ? dataMap.get("so_no") : dataMap.get("soNo");
                    if (soNoObj != null) {
                        psiOrderNo = soNoObj.toString();
                    }
                }
                erpDistributionOrderService.markPsiOk(erpOrder.getId(), psiOrderId, psiOrderNo);
            }
            bool = true;
        }else{
            if (erpGoodsOrderQueue != null) {
                erpGoodsOrderQueue.setStatus("2");
                erpGoodsOrderQueue.setMsg("失败："+JsonUtil.transferToJson(resultMap));
                erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
            }
            updatePsiSyncLog(logId, psiProduct.get("about_id").toString(), "1",
                    quantity.subtract(BigDecimal.ONE).toString(), quantity.toString(),
                    "500", JsonUtil.transferToJson(resultMap));
            bool = false;
        }
        return bool;
    }


    /** 订单号序列 */
    private static final java.util.concurrent.atomic.AtomicInteger DISTRIBUTION_ORDER_SEQ =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /**
     * 生成 ERP 分销订单主键（时间戳 + 序列，避免前端 JS 精度问题由接口层转字符串）
     */
    private Long nextDistributionOrderId() {
        long ts = System.currentTimeMillis();
        int seq = DISTRIBUTION_ORDER_SEQ.incrementAndGet() & 0xFFFFF;
        return (ts << 20) | seq;
    }

    /**
     * 生成 ERP 分销订单号：FX + yyyyMMddHHmmss + 6位序列
     */
    private String nextDistributionOrderNo() {
        String ts = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int seq = DISTRIBUTION_ORDER_SEQ.incrementAndGet() % 1000000;
        return "FX" + ts + String.format("%06d", seq);
    }

    /**
     * 创建（或复用）ERP 分销订单。
     * 返回 null 表示该本已分账/已下发，应跳过；
     * 返回 id 为 null 表示创建失败。
     */
    private ErpDistributionOrder prepareErpDistributionOrder(ErpGoodsOrder erpGoodsOrder, Map psiProduct,
                                                             GoodsDto goodsDto, int itemSeq,
                                                             BigDecimal unitPrice, BigDecimal freight,
                                                             BigDecimal bookPrice, BigDecimal platformFee,
                                                             BigDecimal warehouseFee, BigDecimal totalAmount,
                                                             PsiEmployees psiEmployees,
                                                             PsiSplitAccountConfig cfgWarehouse,
                                                             PsiSplitAccountConfig cfgPlatform) {
        try {
            Long sellerId = Long.parseLong(psiProduct.get("about_id").toString());
            Long productId = Long.parseLong(psiProduct.get("id").toString());

            ErpDistributionOrder exist = erpDistributionOrderService.getBySourceAndItem(erpGoodsOrder.getId(), productId, itemSeq);
            if (exist != null) {
                // 已存在：交给调用方按状态判断（已下发则结束队列，分账失败可复用重跑）
                return exist;
            }

            ErpDistributionOrder order = new ErpDistributionOrder();
            order.setId(nextDistributionOrderId());
            order.setOrderNo(nextDistributionOrderNo());
            order.setSourceOrderId(erpGoodsOrder.getId());
            order.setSourceOrderSn(erpGoodsOrder.getOrderSn());
            order.setQueueId(erpGoodsOrder.getQueueId() == null ? null : Long.parseLong(erpGoodsOrder.getQueueId()));
            order.setOwnerId(erpGoodsOrder.getCreatedBy());
            order.setSellerId(sellerId);

            SysUser payUser = userService.selectUserOne(erpGoodsOrder.getCreatedBy());
            SysUser sellerUser = userService.selectUserOne(sellerId);
            order.setBuyerPhone(payUser == null ? null : payUser.getPhonenumber());
            order.setSellerPhone(sellerUser == null ? null : sellerUser.getPhonenumber());

            order.setShopErpId(erpGoodsOrder.getShopErpId());
            order.setShopErpName(erpGoodsOrder.getShopErpName());
            order.setShopType(erpGoodsOrder.getShopType() == null ? null : erpGoodsOrder.getShopType().intValue());
            order.setProductId(productId);
            order.setBarcode(psiProduct.get("barcode") == null ? null : psiProduct.get("barcode").toString());
            order.setProductName(psiProduct.get("name") == null ? goodsDto.getGoodsName() : psiProduct.get("name").toString());
            order.setAppearance(psiProduct.get("appearance") == null ? null : psiProduct.get("appearance").toString());

            order.setItemSeq(itemSeq);
            order.setQuantity(1);
            order.setUnitPrice(unitPrice == null ? 0L : unitPrice.longValue());
            order.setFreight(freight == null ? 0L : freight.longValue());
            order.setBookPrice(bookPrice == null ? 0L : bookPrice.longValue());
            order.setPlatformFee(platformFee == null ? 0L : platformFee.longValue());
            order.setWarehouseFee(warehouseFee == null ? 0L : warehouseFee.longValue());
            order.setTotalAmount(totalAmount == null ? 0L : totalAmount.longValue());

            order.setSplitConfigId(psiEmployees.getSplitAccountConfigId());
            order.setSplitConfigName(psiEmployees.getRuleName());
            Map ruleMap = new LinkedHashMap();
            ruleMap.put("warehouse", cfgWarehouse);
            ruleMap.put("platform", cfgPlatform);
            order.setSplitRule(JsonUtil.transferToJson(ruleMap));

            // 商品 + 收件信息快照（仓库方发货用）
            Map snapshot = new LinkedHashMap();
            snapshot.put("goodsId", goodsDto.getGoodsId());
            snapshot.put("goodsName", goodsDto.getGoodsName());
            snapshot.put("goodsCount", goodsDto.getGoodsCount());
            snapshot.put("goodsPrice", goodsDto.getGoodsPrice());
            snapshot.put("goodsImgs", goodsDto.getGoodsImgs());
            // 平台订单金额 / 实付金额（买家真正支付的钱，用于 ERP 分销订单展示）
            snapshot.put("orderAmount", erpGoodsOrder.getOrderTotal());
            snapshot.put("payAmount", erpGoodsOrder.getPayAmount());
            snapshot.put("outerGoodsId", goodsDto.getOuterGoodsId());
            snapshot.put("outerId", goodsDto.getOuterId());
            snapshot.put("receiverName", erpGoodsOrder.getReceiverName());
            snapshot.put("receiverPhone", erpGoodsOrder.getMobile());
            snapshot.put("province", erpGoodsOrder.getProvince());
            snapshot.put("city", erpGoodsOrder.getCity());
            snapshot.put("country", erpGoodsOrder.getCountry());
            snapshot.put("town", erpGoodsOrder.getTown());
            order.setItemList(JsonUtil.transferToJson(snapshot));

            order.setStatus(ErpDistributionOrder.STATUS_PENDING);
            order.setRetryCount(0);
            order.setCreateBy(erpGoodsOrder.getCreatedBy());
            order.setCreateTime(new Date());
            order.setUpdateTime(new Date());
            order.setDelFlag("0");
            order.setTenantId("000000");
            erpDistributionOrderService.create(order);
            writeDistLog(order, ErpDistributionOrderLog.CATEGORY_ORDER, ErpDistributionOrderLog.ACTION_ORDER_CREATE,
                    null, ErpDistributionOrder.STATUS_PENDING,
                    order.getOwnerId(), order.getTotalAmount(), ErpDistributionOrderLog.SETTLE_NONE,
                    ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "自动创建ERP分销订单");
            return order;
        } catch (Exception e) {
            log.error("创建ERP分销订单失败, orderSn={}", erpGoodsOrder.getOrderSn(), e);
            ErpDistributionOrder failed = new ErpDistributionOrder();
            failed.setId(null);
            return failed;
        }
    }

    /**
     * 分销分账（5 笔资金操作 + 5 条分账日志）
     */
    private void doSplitAccount(ErpGoodsOrder erpGoodsOrder, Map psiProduct, Object warehouses,
                                SysUser sysUser, BigDecimal balance, SysUser adminUser,
                                Long warehouseUserId, BigDecimal price,
                                BigDecimal handlingFeePlatform, BigDecimal handlingFeeWarehouse,
                                BigDecimal totalPrice, PsiEmployees psiEmployees,
                                PsiSplitAccountConfig psiSplitAccountConfigWarehouse,
                                PsiSplitAccountConfig psiSplitAccountConfigPlatform,
                                String businessNo) {
        // 分账日志对象
        Map createSplitAccountDeductionLog = new HashMap();
        // 订单号标识
        createSplitAccountDeductionLog.put("business_no", businessNo);
        // 分账配置id
        createSplitAccountDeductionLog.put("config_id", psiEmployees.getSplitAccountConfigId());
        // 分账配置名称
        createSplitAccountDeductionLog.put("config_name", psiEmployees.getRuleName());
        // 类型
        createSplitAccountDeductionLog.put("status", "0");
        // 规则信息
        JSONObject deductionDetails = new JSONObject();
        // 订单id
        deductionDetails.put("erpOrderId", erpGoodsOrder.getId());
        // 分账规则 仓库方
        deductionDetails.put("psiSplitAccountConfigWarehouse", psiSplitAccountConfigWarehouse);
        // 分账规则 分润方
        deductionDetails.put("psiSplitAccountConfigPlatform", psiSplitAccountConfigPlatform);
        // 商品信息
        deductionDetails.put("product", psiProduct);
        // 仓库方手续费
        deductionDetails.put("handlingFeeWarehouse", handlingFeeWarehouse);
        // 分润方手续费
        deductionDetails.put("handlingFeePlatform", handlingFeePlatform);
        // 仓库信息
        deductionDetails.put("warehouses", warehouses);
        /**
         * 用户部分
         */
        // 1.修改用户余额
        sysUser.setBalance(balance.subtract(totalPrice));
        userService.updateMoney(sysUser);
        deductionDetails.put("msg", "扣款金额");
        deductionDetails.put("usePhone", sysUser.getPhonenumber());
        deductionDetails.put("userId", sysUser.getUserId());
        deductionDetails.put("handlingFee", handlingFeePlatform);
        createSplitAccountDeductionLog(createSplitAccountDeductionLog, balance, "-" + totalPrice, sysUser.getBalance(), sysUser.getUserId(), deductionDetails);
        // 2.修改平台账号
        BigDecimal oldFreeze = adminUser.getFreeze();
        adminUser.setFreeze(adminUser.getFreeze().add(handlingFeePlatform));
        userService.updateMoney(adminUser);
        deductionDetails.put("msg", "冻结资金增加");
        deductionDetails.put("usePhone", adminUser.getPhonenumber());
        deductionDetails.put("userId", adminUser.getUserId());
        deductionDetails.put("fromUser", sysUser.getPhonenumber());
        deductionDetails.put("handlingFee", "");
        createSplitAccountDeductionLog(createSplitAccountDeductionLog, oldFreeze, handlingFeePlatform.toString(), adminUser.getFreeze(), 0L, deductionDetails);
        /**
         * 仓库部分
         */
        SysUser warehouseUser = userService.selectUserOne(warehouseUserId);
        // 1.先增加仓库金额
        BigDecimal oldFreezeWarehouseAdd = warehouseUser.getFreeze();
        warehouseUser.setFreeze(oldFreezeWarehouseAdd.add(price));
        userService.updateMoney(warehouseUser);
        deductionDetails.put("msg", "冻结资金增加");
        deductionDetails.put("usePhone", warehouseUser.getPhonenumber());
        deductionDetails.put("userId", warehouseUser.getUserId());
        deductionDetails.put("fromUser", sysUser.getPhonenumber());
        deductionDetails.put("handlingFee", "");
        createSplitAccountDeductionLog(createSplitAccountDeductionLog, oldFreezeWarehouseAdd, price.toString(), warehouseUser.getFreeze(), warehouseUser.getUserId(), deductionDetails);
        // 2.扣除仓库手续费
        BigDecimal oldFreezeWarehouseSub = warehouseUser.getFreeze();
        warehouseUser.setFreeze(oldFreezeWarehouseSub.subtract(handlingFeeWarehouse));
        userService.updateMoney(warehouseUser);
        deductionDetails.put("msg", "冻结资金扣除手续费");
        deductionDetails.put("usePhone", warehouseUser.getPhonenumber());
        deductionDetails.put("userId", warehouseUser.getUserId());
        deductionDetails.put("fromUser", "");
        deductionDetails.put("handlingFee", handlingFeeWarehouse);
        createSplitAccountDeductionLog(createSplitAccountDeductionLog, oldFreezeWarehouseSub, "-" + handlingFeeWarehouse, warehouseUser.getFreeze(), warehouseUser.getUserId(), deductionDetails);
        // 3.修改平台账号
        BigDecimal oldFreezeAdmin = adminUser.getFreeze();
        adminUser.setFreeze(oldFreezeAdmin.add(handlingFeeWarehouse));
        userService.updateMoney(adminUser);
        deductionDetails.put("msg", "冻结资金增加");
        deductionDetails.put("usePhone", adminUser.getPhonenumber());
        deductionDetails.put("userId", adminUser.getUserId());
        deductionDetails.put("fromUser", warehouseUser.getPhonenumber());
        deductionDetails.put("handlingFee", "");
        createSplitAccountDeductionLog(createSplitAccountDeductionLog, oldFreezeAdmin, handlingFeeWarehouse.toString(), adminUser.getFreeze(), 0L, deductionDetails);
    }

    /**
     * 分销订单退款：出库未完成 -> 立即回滚；出库已完成 -> 置为已退款待处理（等仓库打单处理）
     */
    private void handleDistributionRefund(ErpGoodsOrder erpGoodsOrder, List<ErpDistributionOrder> distOrders) {
        int ok = 0;
        String err = null;
        for (ErpDistributionOrder dist : distOrders) {
            if (dist.getStatus() == null || dist.getStatus() < ErpDistributionOrder.STATUS_PSI_OK) {
                continue;
            }
            try {
                if (dist.getStatus() == ErpDistributionOrder.STATUS_REFUNDED) {
                    ok++;
                    continue;
                }
                if (isOutboundCompleted(dist)) {
                    // 先记录售后（锁定平台退款的申请时间），再置为已退款待处理
                    afterSalesService.recordPlatformRefund(dist.getId());
                    erpDistributionOrderService.updateStatusById(dist.getId(),
                            ErpDistributionOrder.STATUS_REFUND_PENDING, "平台已退款，待仓库打单处理");
                    ok++;
                } else {
                    afterSalesService.autoRefund(dist.getId(), true);
                    ok++;
                }
            } catch (Exception e) {
                err = e.getMessage();
                log.error("分销订单退款处理失败, distId={}", dist.getId(), e);
            }
        }
        if (erpGoodsOrder.getQueueId() != null) {
            ErpGoodsOrderQueue queue = new ErpGoodsOrderQueue();
            queue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
            queue.setStatus(err == null ? "1" : "2");
            queue.setMsg(err == null ? "订单编号：" + erpGoodsOrder.getOrderSn() + ";分销退款处理完成" : "分销退款处理异常：" + err);
            erpGoodsOrderQueueService.update(queue);
        }
    }

    /**
     * 查询 PSI 销售订单状态，判断是否已出库（>=4 已出库/已发货）
     */
    private boolean isOutboundCompleted(ErpDistributionOrder dist) {
        try {
            String res = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                    "/api/sales-order/query-by-erp-id?user_id=" + dist.getSellerId() + "&erp_id=" + dist.getId());
            Map resMap = JsonUtil.transferToObj(res, Map.class);
            if (resMap == null || resMap.get("data") == null) {
                return false;
            }
            List list = (List) resMap.get("data");
            for (Object o : list) {
                Map item = (Map) o;
                Object statusObj = item.get("status") != null ? item.get("status") : item.get("Status");
                if (statusObj != null && Integer.parseInt(statusObj.toString()) >= 4) {
                    return true;
                }
                Object outQty = item.get("outbound_quantity") != null ? item.get("outbound_quantity") : item.get("outboundQuantity");
                if (outQty != null && Integer.parseInt(outQty.toString()) > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("查询PSI出库状态失败, distId={}", dist.getId(), e);
        }
        return false;
    }

    /**
     * 订单完成事件
     */
    /** 把原分账流水标记为已结算（status=1 + msg 后缀），保证重复执行不会重复解冻 */
    private void markSplitLogSettled(Map row, JSONObject details, String origMsg) {
        try {
            JSONObject copy = new JSONObject((Map<String, ?>) details);
            copy.put("msg", (origMsg == null ? "" : origMsg) + "-已结算");
            Map update = new HashMap();
            update.put("id", row.get("id"));
            update.put("status", "1");
            update.put("deduction_details", JsonUtil.transferToJson(copy));
            InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/split-account-deduction-log/update", update);
        } catch (Exception e) {
            log.error("标记分账流水已结算失败 id={}", row.get("id"), e);
        }
    }

    /**
     * 下发 PSI 失败时把本次分账原路冲正：下发都没成功就不该占款。
     * 冲正后分销单回到「下发失败（未分账）」，可直接重新下发（会重新校验库存/余额）。
     */
    private boolean rollbackSplitForFail(ErpDistributionOrder dist, String reason) {
        if (dist == null || dist.getId() == null) {
            return false;
        }
        BigDecimal totalAmount = dist.getTotalAmount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(dist.getTotalAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        boolean done = false;
        try {
            BigDecimal platformFee = dist.getPlatformFee() == null ? BigDecimal.ZERO : BigDecimal.valueOf(dist.getPlatformFee());
            BigDecimal warehouseFee = dist.getWarehouseFee() == null ? BigDecimal.ZERO : BigDecimal.valueOf(dist.getWarehouseFee());
            BigDecimal bookPrice = dist.getBookPrice() == null ? BigDecimal.ZERO : BigDecimal.valueOf(dist.getBookPrice());
            // 平台冻结 = 分润方手续费 + 仓库方手续费；仓库冻结 = 书价 - 仓库方手续费
            BigDecimal platformBack = platformFee.add(warehouseFee);
            BigDecimal sellerBack = bookPrice.subtract(warehouseFee);

            SysUser ownerUser = userService.selectUserOne(dist.getOwnerId());
            if (ownerUser != null) {
                ownerUser.setBalance((ownerUser.getBalance() == null ? BigDecimal.ZERO : ownerUser.getBalance()).add(totalAmount));
                userService.updateMoney(ownerUser);
            }
            SysUser platformUser = userService.selectUserOne(1L);
            if (platformUser != null && platformBack.compareTo(BigDecimal.ZERO) > 0) {
                platformUser.setFreeze((platformUser.getFreeze() == null ? BigDecimal.ZERO : platformUser.getFreeze()).subtract(platformBack));
                userService.updateMoney(platformUser);
            }
            SysUser sellerUser = userService.selectUserOne(dist.getSellerId());
            if (sellerUser != null && sellerBack.compareTo(BigDecimal.ZERO) > 0) {
                sellerUser.setFreeze((sellerUser.getFreeze() == null ? BigDecimal.ZERO : sellerUser.getFreeze()).subtract(sellerBack));
                userService.updateMoney(sellerUser);
            }
            markSplitLogsRolledBack(dist);
            done = true;
            log.warn("下发PSI失败已冲正分账 distId={} 买家退回={} 平台冻结回退={} 仓库冻结回退={} reason={}",
                    dist.getId(), totalAmount, platformBack, sellerBack, reason);
        } catch (Exception e) {
            log.error("下发失败冲正分账异常 distId={}", dist.getId(), e);
        }
        return done;
    }

    /** 把该分销单对应的分账流水标记为已冲正，避免订单完成/退款时对已冲正的钱再操作 */
    private void markSplitLogsRolledBack(ErpDistributionOrder dist) {
        try {
            String goodsId = resolvePlatformGoodsId(dist);
            if (goodsId == null || "null".equals(goodsId) || goodsId.trim().isEmpty()) {
                return;
            }
            String businessNo = dist.getSourceOrderSn() + "-" + dist.getShopErpId() + "-" + goodsId;
            int page = 1;
            while (true) {
                String res = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                        "/api/split-account-deduction-log/list?page=" + page + "&page_size=100&business_no=" + businessNo);
                Map resMap = JsonUtil.transferToObj(res, Map.class);
                Map dataMap = resMap == null ? null : (Map) resMap.get("data");
                List pageList = dataMap == null ? null : (List) dataMap.get("list");
                if (pageList == null || pageList.isEmpty()) {
                    break;
                }
                for (Object o : pageList) {
                    Map row = (Map) o;
                    Object st = row.get("status");
                    if (st != null && "1".equals(st.toString())) {
                        continue;
                    }
                    Map update = new HashMap();
                    update.put("id", row.get("id"));
                    update.put("status", "1");
                    Object detailsObj = row.get("deduction_details");
                    if (detailsObj instanceof Map) {
                        JSONObject details = new JSONObject((Map<String, ?>) detailsObj);
                        String msg = details.get("msg") == null ? "" : details.get("msg").toString();
                        details.put("msg", msg + "-已冲正（下发失败）");
                        update.put("deduction_details", JsonUtil.transferToJson(details));
                    }
                    InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/split-account-deduction-log/update", update);
                }
                if (pageList.size() < 100) {
                    break;
                }
                page++;
            }
        } catch (Exception e) {
            log.error("标记分账流水已冲正失败 distId={}", dist.getId(), e);
        }
    }

    @Override
    public void orderFinish(ErpGoodsOrder erpGoodsOrder){
        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
        // 获取商品信息
        GoodsDto goodsDto = erpGoodsOrder.getGoodsDto();
        if (goodsDto == null){
            goodsDto = JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class);
        }
        writeDistLogBySource(erpGoodsOrder.getId(), ErpDistributionOrderLog.CATEGORY_FUND,
                ErpDistributionOrderLog.ACTION_SETTLE_BEGIN,
                ErpDistributionOrder.STATUS_PSI_OK, ErpDistributionOrder.STATUS_PSI_OK,
                null, null, ErpDistributionOrderLog.SETTLE_SETTLING,
                ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "订单完成，开始结算");
        // 查询分账日志 ，若存在 则将 冻结余额增加的日志，生成 冻结余额 → 余额 的操作
        // 分页取全部分账流水（接口默认每页 20 条，会漏处理；这里按 100/页 循环取完）
        String settleBusinessNo = erpGoodsOrder.getOrderSn()+"-"+erpGoodsOrder.getShopErpId()+"-"+goodsDto.getGoodsId();
        List logList = new java.util.ArrayList();
        int logPage = 1;
        while (true) {
            String res = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                    "/api/split-account-deduction-log/list?page="+logPage+"&page_size=100&business_no="+settleBusinessNo);
            Map resData = JsonUtil.transferToObj(res,Map.class);
            Map resDataMap = resData == null ? null : (Map) resData.get("data");
            List pageList = resDataMap == null ? null : (List) resDataMap.get("list");
            if (pageList == null || pageList.isEmpty()) {
                break;
            }
            logList.addAll(pageList);
            if (pageList.size() < 100) {
                break;
            }
            logPage++;
        }
        java.util.List<String> settleFlows = new java.util.ArrayList<>();
        if (!logList.isEmpty()){
            for (Object object : logList){
                // 列表记录
                Map splitAccountDeductionLog = (Map) object;
                // 已结算/已售后的分账流水跳过，避免重复解冻或重复扣款
                Object logStatus = splitAccountDeductionLog.get("status");
                if (logStatus != null && "1".equals(logStatus.toString())) {
                    continue;
                }
                // 日志信息
                Object deductionDetailsObj = splitAccountDeductionLog.get("deduction_details");
                JSONObject deductionDetails = new JSONObject((Map<String, ?>) deductionDetailsObj);
                String origMsg = deductionDetails.get("msg") == null ? "" : deductionDetails.get("msg").toString();

                if (deductionDetails.get("msg").toString().contains("冻结资金增加")){
                    // 交互金额 单位 分
                    BigDecimal deductionAmount = new BigDecimal(splitAccountDeductionLog.get("deduction_amount").toString()).multiply(new BigDecimal(100));
                    // 获取操作人id
                    String userId = splitAccountDeductionLog.get("created_by").toString();
                    if (userId.equals("0")){
                        userId = "1";
                    }
                    // 获取操作人信息
                    SysUser user = userService.selectUserOne(Long.parseLong(userId));
                    // 原冻结资金
                    BigDecimal oldFreeze = user.getFreeze();
                    // 新冻结金额
                    user.setFreeze(oldFreeze.subtract(deductionAmount));
                    // 执行修改
                    userService.updateMoney(user);
                    // 冻结资金扣减
                    deductionDetails.put("msg","冻结资金扣减");
                    // 金额来自  扣款不需要
                    deductionDetails.put("fromUser","");
                    // 生成 冻结资金扣减记录
                    createSplitAccountDeductionLog(splitAccountDeductionLog,oldFreeze,"-"+deductionAmount,user.getFreeze(),user.getUserId(),deductionDetails);
                    // 原余额
                    BigDecimal oldBalance = user.getBalance();
                    // 新余额
                    user.setBalance(user.getBalance().add(deductionAmount));
                    // 执行修改
                    userService.updateMoney(user);
                    // 金额来自  扣款不需要
                    deductionDetails.put("fromUser",user.getPhonenumber());
                    // 日志
                    deductionDetails.put("msg","余额增加");
                    // 生成 余额资金增加记录
                    createSplitAccountDeductionLog(splitAccountDeductionLog,oldBalance,deductionAmount+"",user.getBalance(),user.getUserId(),deductionDetails);
                    String settleWho = userId.equals("1") ? "平台" : "仓库";
                    String settleFreezeFlow = flowText(settleWho + "冻结 ", oldFreeze, user.getFreeze());
                    if (settleFreezeFlow != null) { settleFlows.add(settleFreezeFlow); }
                    String settleBalanceFlow = flowText(settleWho + "余额 ", oldBalance, user.getBalance());
                    if (settleBalanceFlow != null) { settleFlows.add(settleBalanceFlow); }
                    // 原流水标记为已结算，订单完成重复触发时不再重复解冻
                    markSplitLogSettled(splitAccountDeductionLog, deductionDetails, origMsg);
                }
            }
            erpGoodsOrderQueue.setStatus("1");
            erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";分销成功");
        }else{
            erpGoodsOrderQueue.setStatus("1");
            erpGoodsOrderQueue.setMsg("订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";非分销商品");
        }
        erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
        // ERP 分销订单置为已完成（仅已下发的行）
        try {
            erpDistributionOrderService.markFinishedBySourceOrder(erpGoodsOrder.getId());
        } catch (Exception e) {
            log.error("标记ERP分销订单已完成失败, orderId={}", erpGoodsOrder.getId(), e);
        }
        writeDistLogBySource(erpGoodsOrder.getId(), ErpDistributionOrderLog.CATEGORY_FUND,
                ErpDistributionOrderLog.ACTION_SETTLE,
                ErpDistributionOrder.STATUS_PSI_OK, ErpDistributionOrder.STATUS_FINISHED,
                null, null, ErpDistributionOrderLog.SETTLE_SETTLED,
                ErpDistributionOrderLog.OPERATOR_SYSTEM, null, joinFlowTexts("结算完成，冻结转余额", settleFlows));
        writeDistLogBySource(erpGoodsOrder.getId(), ErpDistributionOrderLog.CATEGORY_ORDER,
                ErpDistributionOrderLog.ACTION_ORDER_FINISH,
                ErpDistributionOrder.STATUS_PSI_OK, ErpDistributionOrder.STATUS_FINISHED,
                null, null, null,
                ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "订单完成");
    }

    /**
     * 订单退货事件
     */
    @Override
    public void orderReturnh(ErpGoodsOrder erpGoodsOrder){
        // 分销订单走独立退款回滚链路（金额/库存/商品处理规则不同）
        List<ErpDistributionOrder> distOrders = erpDistributionOrderService.listBySourceOrderId(erpGoodsOrder.getId());
        if (distOrders != null && !distOrders.isEmpty()) {
            handleDistributionRefund(erpGoodsOrder, distOrders);
            return;
        }
        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
        // 获取商品信息
        GoodsDto goodsDto = erpGoodsOrder.getGoodsDto();
        if (goodsDto == null){
            goodsDto = JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class);
        }
        writeDistLogBySource(erpGoodsOrder.getId(), ErpDistributionOrderLog.CATEGORY_FUND,
                ErpDistributionOrderLog.ACTION_REFUND_BEGIN,
                null, null, null, null, ErpDistributionOrderLog.SETTLE_REFUNDING,
                ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "开始退款");
        try{
            // 查询分账日志 ，若存在 则将 冻结余额增加的日志，生成 冻结余额 → 余额 的操作
            String res = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/split-account-deduction-log/list?business_no="+erpGoodsOrder.getOrderSn()+"-"+erpGoodsOrder.getShopErpId()+"-"+goodsDto.getGoodsId());
            Map resMap = JsonUtil.transferToObj(res,Map.class);
            Map resData = (Map) resMap.get("data");
            if (resData.get("list") != null){

                List logList = (List) resData.get("list");
                // 获取第一条日志  解析商品的aboutId;
                Map logData = (Map) logList.get(0);
                // 日志信息
                Map detail = (Map) logData.get("deduction_details");
                // 获取商品信息
                Map product = (Map) detail.get("product");
                // 商品的用户id
                String aboutId = product.get("about_id").toString();
                // 商品id
                String productId = product.get("id").toString();

                // 创建PSI库存同步日志
                String logId = savePsiSyncLog(
                        String.valueOf(productId),
                        String.valueOf(aboutId),
                        JsonUtil.transferToJson(erpGoodsOrder),
                        erpGoodsOrder.getItemList(),
                        "解锁库存",
                        String.valueOf(aboutId)
                );

                // 解锁库存：分销订单在 PSI 关联的是 ERP 分销订单，需要按 ERP 单逐张解锁
                boolean unlockOk = true;
                List<ErpDistributionOrder> erpOrders = erpDistributionOrderService.listBySourceOrderId(erpGoodsOrder.getId());
                if (erpOrders == null || erpOrders.isEmpty()) {
                    // 自营订单：仍按平台订单关联解锁
                    Map unlockInventoryMap = new HashMap();
                    unlockInventoryMap.put("association_order_id", erpGoodsOrder.getId());
                    unlockInventoryMap.put("association_order_no", erpGoodsOrder.getOrderSn());
                    unlockInventoryMap.put("about_id", aboutId);
                    String unlockInventoryRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/sales-order/unlock-inventory", unlockInventoryMap);
                    Map unlockInventoryResMap = JsonUtil.transferToObj(unlockInventoryRes, Map.class);
                    unlockOk = unlockInventoryResMap != null && unlockInventoryResMap.get("code") != null
                            && unlockInventoryResMap.get("code").toString().equals("200");
                } else {
                    for (ErpDistributionOrder eo : erpOrders) {
                        if (eo.getPsiOrderId() == null) {
                            // 没有成功的 PSI 销售订单，无需解锁
                            continue;
                        }
                        Map unlockInventoryMap = new HashMap();
                        unlockInventoryMap.put("association_order_id", eo.getId());
                        unlockInventoryMap.put("association_order_no", eo.getOrderNo());
                        unlockInventoryMap.put("about_id", aboutId);
                        String unlockInventoryRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/sales-order/unlock-inventory", unlockInventoryMap);
                        Map unlockInventoryResMap = JsonUtil.transferToObj(unlockInventoryRes, Map.class);
                        if (unlockInventoryResMap == null || unlockInventoryResMap.get("code") == null
                                || !unlockInventoryResMap.get("code").toString().equals("200")) {
                            unlockOk = false;
                            log.error("按ERP分销订单解锁库存失败, erpOrderNo={}, res={}", eo.getOrderNo(), unlockInventoryRes);
                        }
                    }
                }
                if (unlockOk){
                    try{
                        for (Object object : logList){
                            // 列表记录
                            Map splitAccountDeductionLog = (Map) object;
                // 已结算/已售后的分账流水跳过，避免重复解冻或重复扣款
                Object logStatus = splitAccountDeductionLog.get("status");
                if (logStatus != null && "1".equals(logStatus.toString())) {
                    continue;
                }
                            // 日志信息
                            Object deductionDetailsObj = splitAccountDeductionLog.get("deduction_details");
                            JSONObject deductionDetails = new JSONObject((Map<String, ?>) deductionDetailsObj);

                            // 交互金额 单位 分
                            BigDecimal deductionAmount = new BigDecimal(splitAccountDeductionLog.get("deduction_amount").toString()).multiply(new BigDecimal(100));
                            // 获取操作人id
                            String userId = splitAccountDeductionLog.get("created_by").toString();
                            if(userId.equals("0")){
                                // 管理员
                                userId = "1";
                            }
                            // 获取操作人信息
                            SysUser user = userService.selectUserOne(Long.parseLong(userId));

                            if (deductionDetails.get("msg").toString().contains("冻结")){
                                // 新冻结金额
                                user.setFreeze(user.getFreeze().subtract(deductionAmount));
                                // 修改
                                userService.updateMoney(user);
                            }else{
                                // 新余额
                                user.setBalance(user.getBalance().subtract(deductionAmount));
                                // 修改
                                userService.updateMoney(user);
                            }
                            // 修改日志记录
                            Map updateLog = new HashMap();
                            updateLog.put("id",splitAccountDeductionLog.get("id").toString());
                            deductionDetails.put("msg",deductionDetails.get("msg") + "-已退款");
                            updateLog.put("deduction_details",deductionDetails);
                            updateLog.put("status","1");
                            String updateLogRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/split-account-deduction-log/update",updateLog);
                            Map updateLogResMap = JsonUtil.transferToObj(updateLogRes,Map.class);
                            if (updateLogResMap.get("code").toString().equals("200")){
                                System.out.println("更新成功");
                            }else{
                                System.out.println("更新失败");
                            }
                        }
                        // 获取库存
                        String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product/getProductInventory?user_id="+aboutId+"&product_id="+productId+"&type=1");
                        Map stockResMap = JsonUtil.transferToObj(stockRes,Map.class);
                        Map stcokData = (Map) stockResMap.get("data");
                        // 库存
                        BigDecimal quantity = new BigDecimal(stcokData.get("quantity").toString());

                        updatePsiSyncLog(logId, String.valueOf(aboutId), "1",
                                quantity+"", quantity.subtract(BigDecimal.ONE)+"",
                                "200", "退款回滚成功");

                        try {
                            // 手动切换到 taskDb
                            DynamicDataSourceContextHolder.push("taskDb");
                            // 同步库存
                            synchronizeStockNew(productId,Long.parseLong(aboutId),Integer.parseInt(goodsDto.getGoodsCount()),quantity.intValue(),erpGoodsOrder);
                            // 根据用户id 商品id 平台id查询 已被删除的记录，给他还原

                            TShopGoodsPublishedDto shopGoodsPublishedDto = null;
                            // 根据商品id获取店铺关联信息
                            if (erpGoodsOrder.getShopType().toString().equals("5")){
                                shopGoodsPublishedDto = selectDelFlag(Long.parseLong(productId),Long.parseLong(aboutId),Long.parseLong(goodsDto.getOuterId()));
                            }else{
                                shopGoodsPublishedDto = selectDelFlag(Long.parseLong(productId),Long.parseLong(aboutId),Long.parseLong(goodsDto.getGoodsId()));
                            }
                            if (shopGoodsPublishedDto != null){
                                updateShopGoodsPublishedRecover(shopGoodsPublishedDto.getId());
                            }
                        } finally {
                            // 使用完后清理
                            DynamicDataSourceContextHolder.poll();
                        }
                        //
                        erpGoodsOrderQueue.setStatus("1");
                        erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";退款回滚成功");

                    }catch (Exception e){
                        updatePsiSyncLog(logId, String.valueOf(aboutId), "1",
                                "", "",
                                "500", "分账/同步库存异常"+e.getMessage());
                        e.printStackTrace();
                        erpGoodsOrderQueue.setStatus("3");
                        erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";分账/同步库存异常："+e.getMessage());
                    }
                }else{
                    updatePsiSyncLog(logId, String.valueOf(aboutId), "1",
                            "", "",
                            "500", "错误提示：解锁库存失败");
                    // 解锁库存失败
                    erpGoodsOrderQueue.setStatus("2");
                    erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";错误提示：解锁库存失败");
                }
            }else{
                // 手动切换到 taskDb
                DynamicDataSourceContextHolder.push("taskDb");
                // 店铺类型
                Long shopType = erpGoodsOrder.getShopType();
                // 获取商品id
                String goodsId = "-1";
                if(shopType == 5){
                    goodsId = goodsDto.getOuterId();        //闲鱼
                }else if(shopType == 1|| shopType == 2){
                    goodsId = goodsDto.getGoodsId();        //拼多多
                }
                // 获取商品属于用户
                try {
                    List<TShopGoodsPublishedDto> tShopGoodsPublishedDtoList = tShopGoodsPublishedMapper.selectByTrilateralId(Long.parseLong(goodsId));
                    if(tShopGoodsPublishedDtoList.isEmpty()){
                        // 查询是否
                        String psiRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/shop-goods-published/by-trilateral?user_id="+erpGoodsOrder.getCreatedBy()+"&trilateral_id="+goodsDto.getGoodsId());
                        Map psiResMap = JsonUtil.transferToObj(psiRes,Map.class);
                        List psiData = (List) psiResMap.get("data");
                        if (psiData.isEmpty()){
                            psiRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/shop-goods-published/by-trilateral?user_id="+erpGoodsOrder.getCreatedBy()+"&trilateral_id="+goodsDto.getOuterId());
                            psiResMap = JsonUtil.transferToObj(psiRes,Map.class);
                            if (psiResMap.get("code").toString().equals("200")){
                                psiData = (List) psiResMap.get("data");
                            }
                        }
                        if (!psiData.isEmpty()){
                            Map data = (Map) psiData.get(0);
                            TShopGoodsPublishedDto tShopGoodsPublishedDto = new TShopGoodsPublishedDto();
                            tShopGoodsPublishedDto.setTrilateralId(Long.parseLong(data.get("trilateral_id").toString()));
                            tShopGoodsPublishedDto.setUserId(erpGoodsOrder.getCreatedBy());
                            tShopGoodsPublishedDto.setProductId(Long.parseLong(data.get("product_id").toString()));
                            tShopGoodsPublishedDto.setIsdistribution("0");
                            tShopGoodsPublishedDtoList.add(tShopGoodsPublishedDto);
                        }
                    }
                    Long aboutId = tShopGoodsPublishedDtoList.get(0).getUserId();
                    Long productId = tShopGoodsPublishedDtoList.get(0).getProductId();


                    Boolean checkOutOfStock = true;

                    try{
                        String checkOutOfStockRes = InterfaceUtils.getInterface("https://psi.api.buzhiyushu.cn","/api/product/check-out-of-stock?about_id="+aboutId+"&goods_id="+productId);
                        Map checkOutOfStockResMap = JsonUtil.transferToObj(checkOutOfStockRes,Map.class);
                        if(checkOutOfStockResMap.get("data") != null){
                            Map data = (Map) checkOutOfStockResMap.get("data");
                            String isOutOfStock = data.get("is_out_of_stock").toString();
                            // isOutOfStock   0 正常  1 销毁
                            if (isOutOfStock.equals("1")){
                                checkOutOfStock = false;
                            }
                        }
                    }catch (Exception e){
                        System.out.println("退款查询商品是否缺货异常：");
                        e.printStackTrace();
                    }

                    if (checkOutOfStock){
                        // 创建PSI库存同步日志
                        String logId2 = savePsiSyncLog(
                                String.valueOf(productId),
                                String.valueOf(aboutId),
                                JsonUtil.transferToJson(erpGoodsOrder),
                                erpGoodsOrder.getItemList(),
                                "解锁库存",
                                String.valueOf(aboutId)
                        );

                        // 解锁库存
                        Map unlockInventoryMap = new HashMap();
                        // 订单id
                        unlockInventoryMap.put("association_order_id",erpGoodsOrder.getId());
                        // 订单号
                        unlockInventoryMap.put("association_order_no",erpGoodsOrder.getOrderSn());
                        // 用户id
                        unlockInventoryMap.put("about_id",aboutId);
                        String unlockInventoryRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/sales-order/unlock-inventory",unlockInventoryMap);
                        Map unlockInventoryResMap = JsonUtil.transferToObj(unlockInventoryRes,Map.class);

                        // 获取库存
                        String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product/getProductInventory?user_id="+aboutId+"&product_id="+productId+"&type=1");
                        Map stockResMap = JsonUtil.transferToObj(stockRes,Map.class);
                        Map stcokData = (Map) stockResMap.get("data");
                        // 库存
                        BigDecimal quantity = new BigDecimal(stcokData.get("quantity").toString());

                        updatePsiSyncLog(logId2, String.valueOf(aboutId), "1",
                                String.valueOf(quantity), quantity.subtract(BigDecimal.ONE).toString(),
                                "200", "退款回滚成功");

                        // 同步库存
                        String log = synchronizeStockNew(productId.toString(),aboutId,Integer.parseInt(goodsDto.getGoodsCount()),quantity.intValue(),erpGoodsOrder);
                        // 退款成功
                        erpGoodsOrderQueue.setStatus("1");
                        erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";退款回滚成功："+log+";");
                    }else{
                        // 创建PSI库存同步日志
                        String logId2 = savePsiSyncLog(
                                String.valueOf(productId),
                                String.valueOf(aboutId),
                                JsonUtil.transferToJson(erpGoodsOrder),
                                erpGoodsOrder.getItemList(),
                                "商品缺货,同步0库存，下架商品",
                                String.valueOf(aboutId)
                        );

                        updatePsiSyncLog(logId2, String.valueOf(aboutId), "1",
                                "0", "0",
                                "200", "退款成功,缺货商品0库存同步");
                        // 同步库存
                        String log = synchronizeStockNew(productId.toString(),aboutId,Integer.parseInt(goodsDto.getGoodsCount()),0,erpGoodsOrder);
                        // 退款成功
                        erpGoodsOrderQueue.setStatus("1");
                        erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";退款成功："+log+";同步0库存");
                    }
                } finally {
                    // 使用完后清理
                    DynamicDataSourceContextHolder.poll();
                }
            }
        }catch (Exception e){
            // 解锁库存失败
            erpGoodsOrderQueue.setStatus("2");
            erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";错误提示："+e.getMessage());
        }
        erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
        // 退款成功则关闭 ERP 分销订单
        if ("1".equals(erpGoodsOrderQueue.getStatus())) {
            try {
                erpDistributionOrderService.markRefundedBySourceOrder(erpGoodsOrder.getId());
                writeDistLogBySource(erpGoodsOrder.getId(), ErpDistributionOrderLog.CATEGORY_FUND,
                        ErpDistributionOrderLog.ACTION_REFUND,
                        ErpDistributionOrder.STATUS_PSI_OK, ErpDistributionOrder.STATUS_REFUNDED,
                        null, null, ErpDistributionOrderLog.SETTLE_REFUNDED,
                        ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "退款完成，资金已退回");
                writeDistLogBySource(erpGoodsOrder.getId(), ErpDistributionOrderLog.CATEGORY_ORDER,
                        ErpDistributionOrderLog.ACTION_ORDER_CLOSE,
                        ErpDistributionOrder.STATUS_PSI_OK, ErpDistributionOrder.STATUS_REFUNDED,
                        null, null, null,
                        ErpDistributionOrderLog.OPERATOR_SYSTEM, null, "退款关闭");
            } catch (Exception e) {
                log.error("关闭ERP分销订单失败, orderId={}", erpGoodsOrder.getId(), e);
            }
        } else {
            writeDistLogBySource(erpGoodsOrder.getId(), ErpDistributionOrderLog.CATEGORY_FUND,
                    ErpDistributionOrderLog.ACTION_REFUND_FAIL,
                    null, null, null, null, ErpDistributionOrderLog.SETTLE_FROZEN,
                    ErpDistributionOrderLog.OPERATOR_SYSTEM, null, erpGoodsOrderQueue.getMsg());
        }
    }


    /**
     * 孔夫子专用订单退货事件
     */
    @Override
    public void orderReturnhKfz(ErpGoodsOrder erpGoodsOrder){
        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
        // 获取商品信息
        GoodsDto goodsDto = erpGoodsOrder.getGoodsDto();
        if (goodsDto == null){
            goodsDto = JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class);
        }
        // 查询分账记录
        String res = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/split-account-deduction-log/list?business_no="+erpGoodsOrder.getOrderSn()+"-"+erpGoodsOrder.getShopErpId()+"-"+goodsDto.getGoodsId());
        Map resMap = JsonUtil.transferToObj(res,Map.class);
        Map resData = (Map) resMap.get("data");
        if (resData.get("list") != null){
            // 如果存在分账代表是分销，执行另一个退款逻辑
            orderReturnh(erpGoodsOrder);
        }else{
            try{
                // 手动切换到 taskDb
                DynamicDataSourceContextHolder.push("taskDb");
                // 店铺类型
                Long shopType = erpGoodsOrder.getShopType();
                // 获取商品id
                String goodsId = "-1";
                if(shopType == 5){
                    goodsId = goodsDto.getOuterId();        //闲鱼
                }else if(shopType == 1|| shopType == 2){
                    goodsId = goodsDto.getGoodsId();        //拼多多
                }
                // 获取商品属于用户
                try {
                    // 查看是否已经出库（分销订单在 PSI 关联的是 ERP 分销订单）
                    List<ErpDistributionOrder> kfzErpOrders = erpDistributionOrderService.listBySourceOrderId(erpGoodsOrder.getId());
                    Long kfzQueryErpId = erpGoodsOrder.getId();
                    if (kfzErpOrders != null && !kfzErpOrders.isEmpty()) {
                        for (ErpDistributionOrder eo : kfzErpOrders) {
                            if (eo.getPsiOrderId() != null) {
                                kfzQueryErpId = eo.getId();
                                break;
                            }
                        }
                    }
                    String salesOrderRes =  InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/sales-order/query-by-erp-id?user_id="+erpGoodsOrder.getCreatedBy()+"&erp_id="+kfzQueryErpId);
                    Map salesOrderResMap =  JsonUtil.transferToObj(salesOrderRes,Map.class);
                    if (salesOrderResMap.get("code").toString().equals("200")){
                        List salesOrderDataList = (List) salesOrderResMap.get("data");
                        Map salesOrderData = (Map) salesOrderDataList.get(0);
                        // 获取出库数量
                        int psiQuantity = Integer.parseInt(salesOrderData.get("quantity").toString());
                        if (psiQuantity == 0){
                            // 出库数量==0 ，代表未出库，解锁库存
                            List<TShopGoodsPublishedDto> tShopGoodsPublishedDtoList = tShopGoodsPublishedMapper.selectByTrilateralId(Long.parseLong(goodsId));
                            Long aboutId = tShopGoodsPublishedDtoList.get(0).getUserId();
                            Long productId = tShopGoodsPublishedDtoList.get(0).getProductId();
                            // 创建PSI库存同步日志
                            String logId2 = savePsiSyncLog(
                                    String.valueOf(productId),
                                    String.valueOf(aboutId),
                                    JsonUtil.transferToJson(erpGoodsOrder),
                                    erpGoodsOrder.getItemList(),
                                    "解锁库存",
                                    String.valueOf(aboutId)
                            );
                            //TODO 需要校验psi是否已经出库，在决定是否解锁库存
                            //解锁库存（分销订单按 ERP 单逐张解锁）
                            if (kfzErpOrders == null || kfzErpOrders.isEmpty()) {
                                Map unlockInventoryMap = new HashMap();
                                unlockInventoryMap.put("association_order_id", erpGoodsOrder.getId());
                                unlockInventoryMap.put("association_order_no", erpGoodsOrder.getOrderSn());
                                unlockInventoryMap.put("about_id", aboutId);
                                InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/sales-order/unlock-inventory", unlockInventoryMap);
                            } else {
                                for (ErpDistributionOrder eo : kfzErpOrders) {
                                    if (eo.getPsiOrderId() == null) {
                                        continue;
                                    }
                                    Map unlockInventoryMap = new HashMap();
                                    unlockInventoryMap.put("association_order_id", eo.getId());
                                    unlockInventoryMap.put("association_order_no", eo.getOrderNo());
                                    unlockInventoryMap.put("about_id", aboutId);
                                    InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/sales-order/unlock-inventory", unlockInventoryMap);
                                }
                            }
                            //获取库存
                            String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/product/getProductInventory?user_id="+aboutId+"&product_id="+productId+"&type=1");
                            Map stockResMap = JsonUtil.transferToObj(stockRes,Map.class);
                            Map stcokData = (Map) stockResMap.get("data");
                            // 库存
                            BigDecimal quantity = new BigDecimal(stcokData.get("quantity").toString());
                            updatePsiSyncLog(logId2, String.valueOf(aboutId), "1",
                                    String.valueOf(quantity), quantity.subtract(BigDecimal.ONE).toString(),
                                    "200", "退款回滚成功");
                        }
                    }

                    // TODO 孔夫子不执行库存同步操作，执行孔夫子店铺库存扣减，因为孔夫子自己会回滚库存
                    String log = kfzStockEdit(erpGoodsOrder,goodsId,-1);
                    // 退款成功
                    erpGoodsOrderQueue.setStatus("1");
                    erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";退款回滚成功："+log+";");
                } finally {
                    // 使用完后清理
                    DynamicDataSourceContextHolder.poll();
                }
            }catch (Exception e){
                // 解锁库存失败
                erpGoodsOrderQueue.setStatus("2");
                erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";错误提示："+e.getMessage());
            }
            erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
            if ("1".equals(erpGoodsOrderQueue.getStatus())) {
                try {
                    erpDistributionOrderService.markRefundedBySourceOrder(erpGoodsOrder.getId());
                } catch (Exception e) {
                    log.error("关闭ERP分销订单失败, orderId={}", erpGoodsOrder.getId(), e);
                }
            }
        }
    }


    /**
     * 手动重新下发 ERP 分销订单
     *  - 状态 3（未分账）：重新入队，由定时任务重跑（复用同一 ERP 单，不会重复建单）
     *  - 状态 1/4（已分账）：只补发 PSI 销售订单，不重复扣款
     */
    @Override
    @DS("master")
    public Map retryDistributionOrder(Long id, Long operatorId) {
        Map result = new HashMap();
        try {
            ErpDistributionOrder erpOrder = erpDistributionOrderService.getById(id);
            if (erpOrder == null) {
                result.put("code", "500");
                result.put("msg", "ERP分销订单不存在");
                return result;
            }
            Integer status = erpOrder.getStatus() == null ? ErpDistributionOrder.STATUS_PENDING : erpOrder.getStatus();
            if (status == ErpDistributionOrder.STATUS_PSI_OK
                    || status == ErpDistributionOrder.STATUS_REFUNDED
                    || status == ErpDistributionOrder.STATUS_FINISHED) {
                result.put("code", "500");
                result.put("msg", "该订单已下发或已关闭，无需重新下发");
                return result;
            }

            erpDistributionOrderService.incrementRetry(id);
            writeDistLog(erpOrder, ErpDistributionOrderLog.CATEGORY_DISPATCH,
                    ErpDistributionOrderLog.ACTION_DISPATCH_RETRY,
                    status, status, erpOrder.getOwnerId(), erpOrder.getTotalAmount(),
                    (status == ErpDistributionOrder.STATUS_SPLIT_OK
                            || status == ErpDistributionOrder.STATUS_FAIL_SPLIT)
                            ? ErpDistributionOrderLog.SETTLE_FROZEN : ErpDistributionOrderLog.SETTLE_NONE,
                    (operatorId != null && operatorId == 1L)
                            ? ErpDistributionOrderLog.OPERATOR_ADMIN : ErpDistributionOrderLog.OPERATOR_BUYER,
                    operatorId, "手动重新下发");

            // 已分账：只补发 PSI
            if (status == ErpDistributionOrder.STATUS_SPLIT_OK
                    || status == ErpDistributionOrder.STATUS_FAIL_SPLIT) {
                return repushPsiSalesOrder(erpOrder, status == ErpDistributionOrder.STATUS_SPLIT_OK);
            }

            // 未分账（待处理 / 分账失败）：重新入队，由定时任务重跑
            if (erpOrder.getQueueId() == null) {
                result.put("code", "500");
                result.put("msg", "该订单缺少队列记录，无法重新入队");
                return result;
            }
            ErpGoodsOrderQueue queue = new ErpGoodsOrderQueue();
            queue.setId(erpOrder.getQueueId());
            queue.setStatus("0");
            queue.setMsg("手动重新下发");
            erpGoodsOrderQueueService.update(queue);
            // 清理防重复执行缓存（30 秒 TTL，避免刚失败时重发被拦）
            try {
                String platformGoodsId = resolvePlatformGoodsId(erpOrder);
                if (platformGoodsId != null) {
                    redisService.delete(erpOrder.getSourceOrderId() + ":" + platformGoodsId);
                }
            } catch (Exception ignore) {
            }
            result.put("code", "200");
            result.put("msg", "已重新加入推送队列，稍后自动重试");
            return result;
        } catch (Exception e) {
            log.error("手动重新下发ERP分销订单失败, id={}", id, e);
            result.put("code", "500");
            result.put("msg", "重新下发异常：" + e.getMessage());
            return result;
        }
    }

    /**
     * 已分账订单：只补发 PSI 销售订单
     *
     * @param syncStock 是否还需要同步平台库存（状态1=未尝试过同步，状态4=已同步过）
     */
    private Map repushPsiSalesOrder(ErpDistributionOrder erpOrder, boolean syncStock) {
        Map result = new HashMap();
        ErpGoodsOrder order = erpGoodsOrderMapper.selectById(erpOrder.getSourceOrderId());
        if (order == null) {
            boolean rolledBack = rollbackSplitForFail(erpOrder, "平台订单不存在，无法补发");
            erpDistributionOrderService.markFail(erpOrder.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                    ErpDistributionOrder.FAIL_STEP_PSI,
                    "平台订单不存在，无法补发" + (rolledBack ? "（分账已冲正）" : ""));
            result.put("code", "500");
            result.put("msg", "平台订单不存在，无法补发");
            return result;
        }

        String productRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                "/api/product/full_info?user_id=" + erpOrder.getSellerId() + "&product_id=" + erpOrder.getProductId());
        Map productMap = JsonUtil.transferToObj(productRes, Map.class);
        if (productMap == null || productMap.get("code") == null || !"200".equals(productMap.get("code").toString())) {
            boolean rolledBack = rollbackSplitForFail(erpOrder, "货主商品不存在或已下架，无法补发");
            erpDistributionOrderService.markFail(erpOrder.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                    ErpDistributionOrder.FAIL_STEP_PSI,
                    "货主商品不存在或已下架，无法补发" + (rolledBack ? "（分账已冲正）" : ""));
            result.put("code", "500");
            result.put("msg", "货主商品不存在或已下架，无法补发");
            return result;
        }
        Map psiProduct = (Map) productMap.get("data");
        psiProduct.put("about_id", erpOrder.getSellerId().toString());

        BigDecimal quantity = BigDecimal.ONE;
        try {
            String stockRes = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),
                    "/api/product/getProductInventory?user_id=" + erpOrder.getSellerId()
                            + "&product_id=" + erpOrder.getProductId() + "&type=1");
            Map stockMap = JsonUtil.transferToObj(stockRes, Map.class);
            Map stockData = stockMap == null ? null : (Map) stockMap.get("data");
            if (stockData != null) {
                Object qtyObj = stockData.get("product_quantity") != null ? stockData.get("product_quantity") : stockData.get("quantity");
                if (qtyObj != null) {
                    quantity = new BigDecimal(qtyObj.toString());
                }
            }
        } catch (Exception ignore) {
        }

        GoodsDto goodsDto = order.getGoodsDto() != null ? order.getGoodsDto()
                : JsonUtil.transferToObj(order.getItemList(), GoodsDto.class);
        Boolean ok = createSalesOrder(order, psiProduct, goodsDto, quantity, erpOrder);
        if (ok) {
            if (syncStock) {
                try {
                    synchronizeStockNew(erpOrder.getProductId().toString(), erpOrder.getSellerId(),
                            -1, quantity.intValue(), order);
                } catch (Exception e) {
                    log.error("补发PSI后同步平台库存失败, id={}", erpOrder.getId(), e);
                }
            }
            result.put("code", "200");
            result.put("msg", "补发成功");
            return result;
        }
        boolean rolledBack = rollbackSplitForFail(erpOrder, "PSI销售订单创建失败");
        erpDistributionOrderService.markFail(erpOrder.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                ErpDistributionOrder.FAIL_STEP_PSI,
                "PSI销售订单创建失败" + (rolledBack ? "（分账已冲正）" : ""));
        result.put("code", "500");
        result.put("msg", "补发失败：PSI销售订单创建失败" + (rolledBack ? "（分账已冲正）" : ""));
        return result;
    }

    /**
     * 解析平台商品ID（用于清理防重复缓存）
     */
    private String resolvePlatformGoodsId(ErpDistributionOrder erpOrder) {
        try {
            if (erpOrder.getItemList() == null) {
                return null;
            }
            Map itemMap = JsonUtil.transferToObj(erpOrder.getItemList(), Map.class);
            if (itemMap == null) {
                return null;
            }
            Object v = (erpOrder.getShopType() != null && erpOrder.getShopType() == 5)
                    ? itemMap.get("outerId") : itemMap.get("goodsId");
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 新增分账日志
     * @param createSplitAccountDeductionLog
     * @param totalAmount               分润前总金额
     * @param deductionAmount                操作金额
     * @param remainingAmount           操作后总金额
     * @param createdBy                 创建人
     * @param deductionDetails          日志
     */
    public void createSplitAccountDeductionLog(Map createSplitAccountDeductionLog,BigDecimal totalAmount,String deductionAmount,BigDecimal remainingAmount,Long createdBy,JSONObject deductionDetails){

        if (createdBy == 1){
            createdBy = 0L;
        }
        BigDecimal totalAmountNuew = BigDecimal.ZERO;
        if (totalAmount != null){
            totalAmountNuew = totalAmount.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        }
        createSplitAccountDeductionLog.put("total_amount",totalAmountNuew);                             // 原始总金额
        createSplitAccountDeductionLog.put("deduction_amount",new BigDecimal(deductionAmount).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));                  // 扣款金额
        createSplitAccountDeductionLog.put("remaining_amount",remainingAmount.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));            // 扣减后金额
        createSplitAccountDeductionLog.put("created_by",createdBy);         // 备注
        createSplitAccountDeductionLog.put("deduction_details",deductionDetails.toString());
        String res = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/split-account-deduction-log/create",createSplitAccountDeductionLog);
        System.out.println(res);
    }

    @Override
    @DS("taskDb")
    @Transactional(rollbackFor = Exception.class)
    public void publishGoods(Long userId, String productIdStr, String trilateralIdStr,
                             String stockStr, Long shopErpId) {
        String[] productIds = productIdStr.split(",");
        String[] trilateralIds = trilateralIdStr.split(",");
        String[] stocks = stockStr.split(",");

        long now = System.currentTimeMillis() / 1000;

        for (int i = 0; i < productIds.length; i++) {
            Long productId = Long.parseLong(productIds[i].trim());
            Long trilateralId = Long.parseLong(trilateralIds[i].trim());
            int stock = Integer.parseInt(stocks[i].trim());

            // 1. 查询该平台商品已存在的对应关系（复用现有查询，不新增/改动 SQL）
            //    现有 SQL: WHERE trilateral_id = ? AND del_flag = 0 ORDER BY id ASC
            List<TShopGoodsPublishedDto> allExistList = tShopGoodsPublishedMapper.selectByTrilateralId(trilateralId);
            List<TShopGoodsPublishedDto> existList = new ArrayList<>();
            if (allExistList != null) {
                for (TShopGoodsPublishedDto existDto : allExistList) {
                    if (existDto.getErpShopId() != null && existDto.getErpShopId().equals(shopErpId)) {
                        existList.add(existDto);
                    }
                }
            }
            int existCount = existList.size();

            // 2. stock <= 0：与原来一致，不做任何操作
            if (stock <= 0) {
                log.info("publishGoods stock={}, 跳过处理 erp_shop_id={}, product_id={}, trilateral_id={}",
                        stock, shopErpId, productId, trilateralId);
                continue;
            }

            // 3. 已存在条数 >= stock：只增不减，不做操作
            if (existCount >= stock) {
                log.info("publishGoods 已存在{}条 >= stock={}, 不需要补齐 erp_shop_id={}, product_id={}, trilateral_id={}",
                        existCount, stock, shopErpId, productId, trilateralId);
                continue;
            }

            // 4. 查不到记录：与原来一致，按库存数量插入；有记录：复制第一条（除 id 外一致）补齐到 stock 条
            List<TShopGoodsPublishedDto> insertList = new ArrayList<>(stock - existCount);
            if (existCount == 0) {
                for (int j = 0; j < stock; j++) {
                    TShopGoodsPublishedDto dto = new TShopGoodsPublishedDto();
                    dto.setErpShopId(shopErpId);
                    dto.setProductId(productId);
                    dto.setTrilateralId(trilateralId);
                    dto.setUserId(userId);
                    dto.setIsdistribution("0");
                    dto.setCreateTime(now);
                    insertList.add(dto);
                }
                log.info("publishGoods 未查询到记录, 按库存插入 erp_shop_id={}, product_id={}, trilateral_id={}, 插入{}条",
                        shopErpId, productId, trilateralId, insertList.size());
            } else {
                TShopGoodsPublishedDto tpl = existList.get(0);
                int need = stock - existCount;
                for (int j = 0; j < need; j++) {
                    TShopGoodsPublishedDto dto = new TShopGoodsPublishedDto();
                    dto.setErpShopId(tpl.getErpShopId());
                    dto.setShopName(tpl.getShopName());
                    dto.setErpShopName(tpl.getErpShopName());
                    dto.setProductId(tpl.getProductId());
                    dto.setWarehouseId(tpl.getWarehouseId());
                    dto.setShopId(tpl.getShopId());
                    dto.setTrilateralId(tpl.getTrilateralId());
                    dto.setUserId(tpl.getUserId());
                    dto.setIsdistribution(tpl.getIsdistribution());
                    dto.setDelFlag(tpl.getDelFlag());
                    dto.setCreateTime(now);
                    dto.setUpdateTime(tpl.getUpdateTime());
                    insertList.add(dto);
                }
                log.info("publishGoods 已存在{}条, 复制补齐{}条 erp_shop_id={}, product_id={}, trilateral_id={}",
                        existCount, need, shopErpId, productId, trilateralId);
            }

            int inserted = tShopGoodsPublishedMapper.batchInsert(insertList);
            log.info("publishGoods 插入 erp_shop_id={}, product_id={}, trilateral_id={}, 插入{}条",
                    shopErpId, productId, trilateralId, inserted);
        }
    }
}