package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.dto.GoodsDto;
import com.order.main.dto.TShopGoodsPublishedDto;
import com.order.main.entity.*;
import com.order.main.mapper.TShopGoodsPublishedMapper;
import com.order.main.service.*;
import com.order.main.util.InterfaceUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import com.sun.jna.platform.mac.SystemB;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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
    public List<TShopGoodsPublishedDto> selectByProductId(Long productId){
        return tShopGoodsPublishedMapper.selectByProductId(productId);
    }


    @Override
    @DS("taskDb")
    public int update(Long id){
        return tShopGoodsPublishedMapper.updateShopGoodsPublished(id);
    }

    @Override
    @DS("taskDb")
    public int deleteById(Long id){
        return tShopGoodsPublishedMapper.deleteById(id);
    }

    @Override
    @DS("taskDb")
    public void createSalesOrder(ErpGoodsOrder erpGoodsOrder) {
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
                if (tShopGoodsPublishedDtoList.isEmpty()){
                    goodsId = goodsDto.getOuterId();
                    tShopGoodsPublishedDtoList = selectByTrilateralId(Long.parseLong(goodsId));
                }
                if (!tShopGoodsPublishedDtoList.isEmpty()){
                    // 获取库存持有人
                    TShopGoodsPublishedDto tShopGoodsPublishedDto = tShopGoodsPublishedDtoList.get(0);
                    String userId = tShopGoodsPublishedDto.getUserId().toString();
                    String productId = tShopGoodsPublishedDto.getProductId().toString();
                    // 获取库存
                    String stockRes = InterfaceUtils.getInterface("https://psi.api.buzhiyushu.cn","/api/product/getProductInventory?user_id="+userId+"&product_id="+productId);
                    Map stockResMap = JsonUtil.transferToObj(stockRes,Map.class);
                    Map stcokData = (Map) stockResMap.get("data");
                    // 原始库存
                    BigDecimal quantity = new BigDecimal(stcokData.get("quantity").toString());
                    // 订单库存
                    int goodsCount = Integer.parseInt(goodsDto.getGoodsCount());
                    // 扣减后库存
                    int inventory = quantity.subtract(new BigDecimal(goodsCount)).intValue();
                    // 发送销售订单数量
                    int orderRunNum = 0;
                    // 存在数据
                    for (int i=0;i < goodsCount && i < tShopGoodsPublishedDtoList.size();i++){
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
                        requestParams.put("items[0][product_id]",tShopGoodsPublishedDto.getProductId().toString());
                        // 单价
                        requestParams.put("items[0][unit_price]", new BigDecimal(goodsDto.getGoodsPrice()).longValue() +"");
                        // 数量
                        requestParams.put("items[0][quantity]","1");
                        // 店铺名
                        requestParams.put("sales_person", erpGoodsOrder.getShopErpName());
                        // 店铺id
                        requestParams.put("sales_person_id", erpGoodsOrder.getShopErpId().toString());
                        // 店铺创建id
                        requestParams.put("about_id", userId);
                        // 店铺类型
                        requestParams.put("shop_type",erpGoodsOrder.getShopType().toString());
                        // 收货人姓名
                        requestParams.put("receiver_name",erpGoodsOrder.getReceiverName());
                        // 收货人电话
                        requestParams.put("receiver_phone",erpGoodsOrder.getMobile());
                        // 收货地址
                        requestParams.put("receiver_address",erpGoodsOrder.getProvince()+"-"+erpGoodsOrder.getCity()+"-"+erpGoodsOrder.getCountry()+"-"+erpGoodsOrder.getTown());
                        // 调用远程接口
                        System.out.println("推送订单数据："+JsonUtil.transferToJson(requestParams));
                        String result = InterfaceUtils.postFormWithSign(
                                "https://psi.api.buzhiyushu.cn/api/sales-order/create",
                                null,
                                requestParams,
                                "",
                                "md5"
                        );
                        Map resultMap = JsonUtil.transferToObj(result, Map.class);
                        ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
                        erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
                        if (resultMap.get("code").toString().equals("200")){
                            orderRunNum++;
                            erpGoodsOrderQueue.setStatus("1");
                            update(tShopGoodsPublishedDto.getId());
                        }else{
                            erpGoodsOrderQueue.setStatus("2");
                        }
                        erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
                        System.out.println(resultMap);
                    }
                    if (orderRunNum > 0){
                        if (inventory < 0){
                            inventory = 0;
                        }
                        // 执行库存同步
                        String log = synchronizeStockNew(productId,inventory,quantity.intValue(),erpGoodsOrder.getId());
                        System.out.println(log);
                    }else{
                        System.out.println("未下发");
                    }
                }else{
                    ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
                    erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
                    erpGoodsOrderQueue.setStatus("3");
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
     * @param erpGoodsId        订单id
     * @return
     */
    @Override
    @DS("taskDb")
    public String synchronizeStockNew(String productId,int inventory,int oldInventory,Long erpGoodsId){
        String log = "库存同步操作记录：";
        // 根据商品id获取店铺关联信息
        List<TShopGoodsPublishedDto> shopGoodsPublishedDtoList = selectByProductId(Long.parseLong(productId));

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

            List<SynchronizationShopLog> synchronizationShopLogsList = new ArrayList<>();
            // 循环已发布商品记录，将所有已发布商品进行库存同步
            for (TShopGoodsPublishedDto sgp : shopGoodsPublishedDtoList){
                // 创建库存同步日志对象
                SynchronizationShopLog synchronizationShopLog = new SynchronizationShopLog();
                // 商品id
                synchronizationShopLog.setGoodsId(sgp.getProductId());
                // 商品创建人
                synchronizationShopLog.setGoodsCreateBy(sgp.getUserId());
                // erp订单id
                synchronizationShopLog.setErpOrderId(erpGoodsId);
                // 更新后库存
                synchronizationShopLog.setInventory(inventory);
                // 更新前库存
                synchronizationShopLog.setInventoryOld(oldInventory);
                // 三方平台id
                synchronizationShopLog.setPlatformId(sgp.getTrilateralId());
                if (oldInventory > inventory){
                    // 扣减库存
                    synchronizationShopLog.setUpdateType("扣减库存");
                }else if (oldInventory < inventory){
                    // 增加库存
                    synchronizationShopLog.setUpdateType("增加库存");
                }else if(oldInventory == inventory){
                    // 同步库存
                    synchronizationShopLog.setUpdateType("同步库存");
                }
                for (Shop shop : shopList){
                    if (shop.getId().equals(sgp.getErpShopId())){
                        // 店铺id
                        synchronizationShopLog.setShopId(shop.getId());
                        // 店铺名称
                        synchronizationShopLog.setShopName(shop.getShopName());
                        // 店铺类型
                        synchronizationShopLog.setShopType(shop.getShopType());
                        // 店铺创建人
                        synchronizationShopLog.setShopCreateBy(Long.parseLong(shop.getCreateBy()));
                        Map resultMap = new HashMap();
                        if(shop.getShopType().equals("1")){
                            // 调用拼多多修改库存
                            resultMap = editStockService.pddEditStock(shop,sgp.getTrilateralId().toString(),inventory+"");
                            log += "拼多多店铺："+shop.getShopName() +":"+resultMap.get("msg")+";";
                        } else if (shop.getShopType().equals("2")){
                            // 调用孔夫子修改库存
                            resultMap = editStockService.kfzEditStock(shop.getToken(),sgp.getTrilateralId().toString(),inventory+"");
                            log += "孔夫子店铺："+shop.getShopName() +":"+resultMap.get("msg")+";";
                        } else if (shop.getShopType().equals("5")){
                            // 调用闲鱼修改库存
                            resultMap = editStockService.xyEditStock(shop,sgp.getTrilateralId().toString(),inventory+"");
                            log += "闲鱼店铺："+shop.getShopName() +":"+resultMap.get("msg")+";";
                        }
                        // 状态码
                        synchronizationShopLog.setCode(resultMap.get("code").toString());
                        // 日志
                        String msg = resultMap.get("msg").toString();
                        if(msg.contains("http")){
                            msg = "接口调用异常，请联系管理员";
                        }
                        synchronizationShopLog.setMsg(msg);
                        // 创建时间
                        long currentTime = System.currentTimeMillis() / 1000;
                        synchronizationShopLog.setCreateAt(currentTime);
                        synchronizationShopLogsList.add(synchronizationShopLog);
                        break;
                    }
                }
            }
            try{
                // 执行批量添加
                synchronizationShopLogService.saveBatch(synchronizationShopLogsList);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        // 返回日志信息
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
                "https://psi.api.buzhiyushu.cn/api/sales-order/create",
                null,
                requestParams,
                "",
                "md5"
        );
        Map resultMap = JsonUtil.transferToObj(result, Map.class);
        System.out.println(resultMap);
    }
}