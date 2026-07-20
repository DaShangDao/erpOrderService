package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.KfzSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.dto.TShopGoodsPublishedDto;
import com.order.main.entity.*;
import com.order.main.mapper.TShopGoodsPublishedMapper;
import com.order.main.service.*;
import com.order.main.util.ClientConstantUtils;
import com.order.main.util.InterfaceUtils;
import com.order.main.util.MaskUtils;
import com.order.main.util.UrlUtil;
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
    public List<TShopGoodsPublishedDto> selectByProductId(Long productId,Long userId){
        return tShopGoodsPublishedMapper.selectByProductId(productId,userId);
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
    public void createSalesOrder(ErpGoodsOrder erpGoodsOrder,WarehouseSettings warehouseSettings) {
        try{
            System.out.println("【开始执行推送销售订单操作】-----------------------："+JsonUtil.transferToJson(erpGoodsOrder));
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
                    tShopGoodsPublishedDtoList = selectByTrilateralId(Long.parseLong(goodsId));
                }
                if (!tShopGoodsPublishedDtoList.isEmpty()){
                    // 获取库存持有人
                    TShopGoodsPublishedDto tShopGoodsPublishedDto = tShopGoodsPublishedDtoList.get(0);
                    distribution(warehouseSettings,erpGoodsOrder,tShopGoodsPublishedDto);
                }else{
                    ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
                    erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
                    erpGoodsOrderQueue.setStatus("3");
                    erpGoodsOrderQueue.setMsg("异常:未找到发货记录");
                    erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
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
    @DS("taskDb")
    public String synchronizeStockNew(String productId,Long userId,int inventory,int oldInventory,ErpGoodsOrder erpGoodsOrder){
        String log = "库存同步操作记录：";
        // 根据商品id获取店铺关联信息
        List<TShopGoodsPublishedDto> shopGoodsPublishedDtoList = selectByProductId(Long.parseLong(productId),userId);
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
//            List<SynchronizationShopLog> synchronizationShopLogsList = new ArrayList<>();
            // 循环已发布商品记录，将所有已发布商品进行库存同步
            for (TShopGoodsPublishedDto sgp : shopGoodsPublishedDtoList){
                if (Objects.equals(sgp.getErpShopId(), erpGoodsOrder.getShopErpId())){
                    continue;
                }
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
                for (Shop shop : shopList){
                    if (shop.getId().equals(sgp.getErpShopId())){
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
                        if(shop.getShopType().equals("1")){
                            // 获取库存
                            String pddRes = InterfaceUtils.getInterface("http://pdd.buzhiyushu.cn","/api/pdd/auth/getShopGoodsDetail?accessToken="+shop.getToken()+"&goodsId="+sgp.getTrilateralId());
                            Map pddResMap = JsonUtil.transferToObj(pddRes,Map.class);
                            if (pddResMap.get("error_response") == null){
                                Map goodsDetailGetResponse = (Map) pddResMap.get("goods_detail_get_response");
                                List skuList = (List) goodsDetailGetResponse.get("sku_list");
                                Map skuMap = (Map) skuList.get(0);
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
                                resultMap = editStockService.pddEditStock(shop,sgp.getTrilateralId().toString(),inventoryNew+"",1);
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
                                // 调用闲鱼修改库存
                                resultMap = editStockService.xyEditStock(shop,sgp.getTrilateralId().toString(),inventoryNew+"");
                                log += "闲鱼店铺："+shop.getShopName() +":"+resultMap.get("msg")+";原库存："+stock+";现库存："+inventoryNew;
                            }else{

                                resultMap = new HashMap();
                                resultMap.put("code","500");
                                resultMap.put("msg",resMap.get("msg"));

                                log += "闲鱼店铺："+shop.getShopName() +";查询商品异常："+resMap.get("msg");
                            }
                        }
                        // 状态码
                        synchronizationShopLogUpdate.put("code",resultMap.get("code").toString());
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
                        break;
                    }
                }
            }
        }

        // 返回日志信息
        return log;
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
        synchronizationShopLogUpdate.put("code",resultMap.get("code").toString());
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
    public void createSalesOrder(String orderId,String orderSn,String productId,String unitPrice,
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
        System.out.println(resultMap);
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
                if (psiEmployees.getRuleValue() != null){
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
                // 循环库存数量
                for (int i=0;i<goodsCount;i++){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // 防重复执行校验：根据订单ID+平台商品ID缓存判断
                    String dedupKey = erpGoodsOrder.getId() + ":" + goodsId;
                    if (redisService.get(dedupKey) != null) {
                        updateQueueStatus(erpGoodsOrder, "2", "失败：重复异常执行");
                        break;
                    }
                    redisService.set(dedupKey, "1", 100, TimeUnit.MILLISECONDS);

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
                                        if (productResMap.get("code").toString().equals("200")){
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
                    Map stcokData = (Map) stockResMap.get("data");
                    // 原始库存
                    BigDecimal quantity = new BigDecimal(stcokData.get("quantity").toString());
                    // 根据收货地获取收费
                    psiProduct.put("cost",getCost(erpGoodsOrder,stockRes));
                    // 仓库价格
                    BigDecimal warehousePrice = new BigDecimal(psiProduct.get("sale_price").toString()).add(new BigDecimal(psiProduct.get("cost").toString()));
                    // 分润方  手续费  （书价（商品价格 + 运费） * 分润方百分比）  +  分润发加价
                    BigDecimal handlingFeePlatform = warehousePrice.multiply(psiSplitAccountConfigPlatform.getRatio()).setScale(0, RoundingMode.CEILING).add(psiSplitAccountConfigPlatform.getAddAmount());
                    // 总价
                    BigDecimal totalPrice = warehousePrice.add(handlingFeePlatform);
                    // 订单金额
                    BigDecimal orderPrice = new BigDecimal(erpGoodsOrder.getOrderTotal());
                    // 差价
                    BigDecimal differencePrice = orderPrice.subtract(totalPrice);
                    if (differencePrice.compareTo(warehouseSettings.getProfitFloor()) >= 0 || psiProduct.get("about_id").equals(erpGoodsOrder.getCreatedBy().toString())){
                        if (quantity.compareTo(BigDecimal.ZERO) > 0){
                            // 商品价格
                            BigDecimal salePrice = new BigDecimal(psiProduct.get("sale_price").toString());
                            // 运费
                            BigDecimal cost = new BigDecimal(psiProduct.get("cost").toString());
                            // 书价
                            BigDecimal price = salePrice.add(cost);
                            // 校验是否是分销商品
                            if(isdistribution.equals("1")){
                                // 仓库方  手续费  （书价（商品价格 + 运费） * 仓库方百分比）  +  仓库方加价
                                BigDecimal handlingFeeWarehouse = price.multiply(psiSplitAccountConfigWarehouse.getRatio()).setScale(0, RoundingMode.CEILING).add(psiSplitAccountConfigWarehouse.getAddAmount());
                                // 校验用户余额是否充足  商品金额（书价+运费） + 手续费
                                if (balance.compareTo(totalPrice) >= 0){
                                    // 查询仓库用户信息
                                    Long warehouseUserId = Long.parseLong(psiProduct.get("about_id").toString());
                                    SysUser  warehouseUser = userService.selectUserOne(warehouseUserId);
                                    // 金额充足 创建销售订单
                                    Boolean bool = createSalesOrder(erpGoodsOrder,psiProduct,goodsDto,quantity);
                                    // 库存同步
                                    String log = synchronizeStockNew(psiProduct.get("id").toString(),warehouseUserId,-1,quantity.intValue(),erpGoodsOrder);
                                    // 校验创建销售订单是否成功
                                    if (bool){
                                        // 分账日志对象
                                        Map createSplitAccountDeductionLog = new HashMap();
                                        // 订单号标识
                                        createSplitAccountDeductionLog.put("business_no",erpGoodsOrder.getOrderSn() + "-" + erpGoodsOrder.getShopErpId() + "-" + goodsDto.getGoodsId());
                                        // 分账配置id
                                        createSplitAccountDeductionLog.put("config_id",psiEmployees.getSplitAccountConfigId());
                                        // 分账配置名称
                                        createSplitAccountDeductionLog.put("config_name",psiEmployees.getRuleName());
                                        // 类型
                                        createSplitAccountDeductionLog.put("status","0");
                                        // 规则信息
                                        JSONObject deductionDetails = new JSONObject();
                                        // 订单id
                                        deductionDetails.put("erpOrderId",erpGoodsOrder.getId());
                                        // 分账规则 仓库方
                                        deductionDetails.put("psiSplitAccountConfigWarehouse",psiSplitAccountConfigWarehouse);
                                        // 分账规则 分润方
                                        deductionDetails.put("psiSplitAccountConfigPlatform",psiSplitAccountConfigPlatform);
                                        // 商品信息
                                        deductionDetails.put("product",psiProduct);
                                        // 仓库方手续费
                                        deductionDetails.put("handlingFeeWarehouse",handlingFeeWarehouse);
                                        // 分润方手续费
                                        deductionDetails.put("handlingFeePlatform",handlingFeePlatform);
                                        // 仓库信息
                                        deductionDetails.put("warehouses",stcokData.get("warehouses"));
                                        /**
                                         * 用户部分
                                         */
                                        // 1.修改用户余额
                                        sysUser.setBalance(balance.subtract(totalPrice));
                                        userService.updateMoney(sysUser);
                                        deductionDetails.put("msg","扣款金额");
                                        deductionDetails.put("usePhone",sysUser.getPhonenumber());
                                        deductionDetails.put("userId",sysUser.getUserId());
                                        deductionDetails.put("handlingFee",handlingFeePlatform);
                                        createSplitAccountDeductionLog(createSplitAccountDeductionLog,balance,"-"+totalPrice,sysUser.getBalance(),sysUser.getUserId(),deductionDetails);
                                        // 2.修改平台账号
                                        BigDecimal oldFreeze = adminUser.getFreeze();
                                        adminUser.setFreeze(adminUser.getFreeze().add(handlingFeePlatform));
                                        userService.updateMoney(adminUser);
                                        deductionDetails.put("msg","冻结资金增加");
                                        deductionDetails.put("usePhone",adminUser.getPhonenumber());
                                        deductionDetails.put("userId",adminUser.getUserId());
                                        deductionDetails.put("fromUser",sysUser.getPhonenumber());
                                        deductionDetails.put("handlingFee","");
                                        createSplitAccountDeductionLog(createSplitAccountDeductionLog,oldFreeze,handlingFeePlatform.toString(),adminUser.getFreeze(),0L,deductionDetails);
                                        /**
                                         * 仓库部分
                                         */
                                        // 1.先增加仓库金额
                                        BigDecimal oldFreezeWarehouseAdd = warehouseUser.getFreeze();
                                        warehouseUser.setFreeze(oldFreezeWarehouseAdd.add(price));
                                        userService.updateMoney(warehouseUser);
                                        deductionDetails.put("msg","冻结资金增加");
                                        deductionDetails.put("usePhone",warehouseUser.getPhonenumber());
                                        deductionDetails.put("userId",warehouseUser.getUserId());
                                        deductionDetails.put("fromUser",sysUser.getPhonenumber());
                                        deductionDetails.put("handlingFee","");
                                        createSplitAccountDeductionLog(createSplitAccountDeductionLog,oldFreezeWarehouseAdd,price.toString(),warehouseUser.getFreeze(),warehouseUser.getUserId(),deductionDetails);
                                        // 2.扣除仓库手续费
                                        BigDecimal oldFreezeWarehouseSub = warehouseUser.getFreeze();
                                        warehouseUser.setFreeze(oldFreezeWarehouseSub.subtract(handlingFeeWarehouse));
                                        userService.updateMoney(warehouseUser);
                                        deductionDetails.put("msg","冻结资金扣除手续费");
                                        deductionDetails.put("usePhone",warehouseUser.getPhonenumber());
                                        deductionDetails.put("userId",warehouseUser.getUserId());
                                        deductionDetails.put("fromUser","");
                                        deductionDetails.put("handlingFee",handlingFeeWarehouse);
                                        createSplitAccountDeductionLog(createSplitAccountDeductionLog,oldFreezeWarehouseSub,"-"+handlingFeeWarehouse,warehouseUser.getFreeze(),warehouseUser.getUserId(),deductionDetails);
                                        // 3.修改平台账号
                                        BigDecimal oldFreezeAdmin = adminUser.getFreeze();
                                        adminUser.setFreeze(oldFreezeAdmin.add(handlingFeeWarehouse));
                                        userService.updateMoney(adminUser);
                                        deductionDetails.put("msg","冻结资金增加");
                                        deductionDetails.put("usePhone",adminUser.getPhonenumber());
                                        deductionDetails.put("userId",adminUser.getUserId());
                                        deductionDetails.put("fromUser",warehouseUser.getPhonenumber());
                                        deductionDetails.put("handlingFee","");
                                        createSplitAccountDeductionLog(createSplitAccountDeductionLog,oldFreezeAdmin,handlingFeeWarehouse.toString(),adminUser.getFreeze(),0L,deductionDetails);
                                    }
                                }else {
                                    updateQueueStatus(erpGoodsOrder, "2", "失败：余额不足");
                                }
                            }else{
                                // 不是分销商品的情况，不需要分账  直接 创建销售订单
                                createSalesOrder(erpGoodsOrder,psiProduct,goodsDto,quantity);
                                String log = synchronizeStockNew(psiProduct.get("id").toString(),erpGoodsOrder.getCreatedBy(),-1,quantity.intValue(),erpGoodsOrder);
                            }
                        }else {
                            updateQueueStatus(erpGoodsOrder, "2", "失败：下发商品库存不足");
                        }
                    }else{
                        updateQueueStatus(erpGoodsOrder, "2", "亏损保护：没有符合的商品进行下发");
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
        Map synchronizationShopLog = new HashMap();
        synchronizationShopLog.put("product_id", productId);
        synchronizationShopLog.put("product_user_id", productUserId);
        synchronizationShopLog.put("erp_order", erpOrderJson);
        synchronizationShopLog.put("platform", platform);
        synchronizationShopLog.put("update_type", updateType);
        synchronizationShopLog.put("shop_id", 1L);
        synchronizationShopLog.put("shop_name", "PSI");
        synchronizationShopLog.put("shop_type", "0");
        synchronizationShopLog.put("shop_create_by", shopCreateBy);
        String addRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(), "/api/synchronization-shop-log/save", synchronizationShopLog);
        Map addResMap = JsonUtil.transferToObj(addRes, Map.class);
        if (addResMap == null || addResMap.get("data") == null) {
            System.out.println("创建PSI库存同步日志失败: " + addRes);
            return null;
        }
        Map addResDataMap = (Map) addResMap.get("data");
        return addResDataMap.get("id") != null ? addResDataMap.get("id").toString() : null;
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
        //关联订单id（平台ID）
        requestParams.put("association_order_id",erpGoodsOrder.getId().toString());
        // 订单编号
        requestParams.put("association_order_no", erpGoodsOrder.getOrderSn());
        // 来源类型 0-预留 1-erp订单
        requestParams.put("from_type", "1");
        // 商品id
        requestParams.put("items[0][product_id]",psiProduct.get("id").toString());
        // 单价
        if (erpGoodsOrder.getShopType() == 2){
            requestParams.put("items[0][unit_price]", erpGoodsOrder.getOrderTotal() +"");
        }else{
            requestParams.put("items[0][unit_price]", new BigDecimal(goodsDto.getGoodsPrice()).longValue() +"");
        }
        // 数量
        requestParams.put("items[0][quantity]","1");
        // 店铺名
        requestParams.put("sales_person", erpGoodsOrder.getShopErpName());
        // 店铺id
        requestParams.put("sales_person_id", erpGoodsOrder.getShopErpId().toString());
        // 店铺创建id
        requestParams.put("about_id", psiProduct.get("about_id").toString());
        // 店铺类型
        requestParams.put("shop_type",erpGoodsOrder.getShopType().toString());
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
        Map resultMap = JsonUtil.transferToObj(result, Map.class);
        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));

        Boolean bool;
        if (resultMap.get("code").toString().equals("200")){
            erpGoodsOrderQueue.setStatus("1");
            erpGoodsOrderQueue.setMsg("创建销售订单成功");
            erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
            updatePsiSyncLog(logId, psiProduct.get("about_id").toString(), "1",
                    quantity.subtract(BigDecimal.ONE).toString(), quantity.toString(),
                    "200", "创建销售订单成功");
            bool = true;
        }else{
            erpGoodsOrderQueue.setStatus("2");
            erpGoodsOrderQueue.setMsg("失败："+JsonUtil.transferToJson(resultMap));
            erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
            updatePsiSyncLog(logId, psiProduct.get("about_id").toString(), "1",
                    quantity.subtract(BigDecimal.ONE).toString(), quantity.toString(),
                    "500", JsonUtil.transferToJson(resultMap));
            bool = false;
        }
        return bool;
    }


    /**
     * 订单完成事件
     */
    @Override
    public void orderFinish(ErpGoodsOrder erpGoodsOrder){
        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
        // 获取商品信息
        GoodsDto goodsDto = erpGoodsOrder.getGoodsDto();
        if (goodsDto == null){
            goodsDto = JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class);
        }
        // 查询分账日志 ，若存在 则将 冻结余额增加的日志，生成 冻结余额 → 余额 的操作
        String res = InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/split-account-deduction-log/list?business_no="+erpGoodsOrder.getOrderSn()+"-"+erpGoodsOrder.getShopErpId()+"-"+goodsDto.getGoodsId());
        Map resData = JsonUtil.transferToObj(res,Map.class);
        Map resDataMap = (Map) resData.get("data");
        if (resDataMap.get("list") != null){
            List logList = (List) resDataMap.get("list");
            for (Object object : logList){
                // 列表记录
                Map splitAccountDeductionLog = (Map) object;
                // 日志信息
                Object deductionDetailsObj = splitAccountDeductionLog.get("deduction_details");
                JSONObject deductionDetails = new JSONObject((Map<String, ?>) deductionDetailsObj);

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
                }
            }
            erpGoodsOrderQueue.setStatus("1");
            erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";分销成功");
        }else{
            erpGoodsOrderQueue.setStatus("1");
            erpGoodsOrderQueue.setMsg("订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";非分销商品");
        }
        erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
    }

    /**
     * 订单退货事件
     */
    @Override
    public void orderReturnh(ErpGoodsOrder erpGoodsOrder){
        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
        // 获取商品信息
        GoodsDto goodsDto = erpGoodsOrder.getGoodsDto();
        if (goodsDto == null){
            goodsDto = JsonUtil.transferToObj(erpGoodsOrder.getItemList(),GoodsDto.class);
        }
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
                if (unlockInventoryResMap.get("code").toString().equals("200")){
                    try{
                        for (Object object : logList){
                            // 列表记录
                            Map splitAccountDeductionLog = (Map) object;
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
                            "500", "错误提示："+unlockInventoryResMap);
                    // 解锁库存失败
                    erpGoodsOrderQueue.setStatus("2");
                    erpGoodsOrderQueue.setMsg( "订单编号："+erpGoodsOrder.getOrderSn()+";商品名称："+goodsDto.getGoodsName()+";错误提示："+unlockInventoryResMap);
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
                    Long aboutId = tShopGoodsPublishedDtoList.get(0).getUserId();
                    Long productId = tShopGoodsPublishedDtoList.get(0).getProductId();

                    // 创建库存同步记录
                    Map synchronizationShopLog = new HashMap();
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
                    // 查看是否已经出库
                    String salesOrderRes =  InterfaceUtils.getInterface(UrlUtil.getNewWarehouse(),"/api/sales-order/query-by-erp-id?user_id="+erpGoodsOrder.getCreatedBy()+"&erp_id="+erpGoodsOrder.getId());
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
                            //解锁库存
                            Map unlockInventoryMap = new HashMap();
                            // 订单id
                            unlockInventoryMap.put("association_order_id",erpGoodsOrder.getId());
                            // 订单号
                            unlockInventoryMap.put("association_order_no",erpGoodsOrder.getOrderSn());
                            // 用户id
                            unlockInventoryMap.put("about_id",aboutId);
                            String unlockInventoryRes = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),"/api/sales-order/unlock-inventory",unlockInventoryMap);
                            Map unlockInventoryResMap = JsonUtil.transferToObj(unlockInventoryRes,Map.class);
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

            // 1. 先删除旧记录
            int deleted = tShopGoodsPublishedMapper.batchDeleteByShopProductTrilateral(shopErpId, productId, trilateralId);
            log.info("publishGoods 删除 erp_shop_id={}, product_id={}, trilateral_id={}, 影响行数={}",
                    shopErpId, productId, trilateralId, deleted);

            if (stock <= 0) {
                log.info("publishGoods stock={}, 跳过插入 product_id={}", stock, productId);
                continue;
            }

            // 2. 按 stock 数量插入多条
            List<TShopGoodsPublishedDto> insertList = new ArrayList<>(stock);
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

            int inserted = tShopGoodsPublishedMapper.batchInsert(insertList);
            log.info("publishGoods 插入 erp_shop_id={}, product_id={}, trilateral_id={}, 插入{}条",
                    shopErpId, productId, trilateralId, inserted);
        }
    }
}