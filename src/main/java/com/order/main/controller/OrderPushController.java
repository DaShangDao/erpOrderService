package com.order.main.controller;

import com.order.main.entity.*;
import com.order.main.service.*;
import com.order.main.util.InterfaceUtils;
import com.order.main.util.UrlUtil;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 手动订单下发Controller
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orderPush")
public class OrderPushController {

    private final IErpGoodsOrderQueueService erpGoodsOrderQueueService;
    private final IErpGoodsOrderService erpGoodsOrderService;
    private final TShopGoodsPublishedService tShopGoodsPublishedService;
    private final ISysUserService sysUserService;
    private final IPsiEmployeesService psiEmployeesService;

    /**
     * 查询商品列表（按ISBN查询进销存，每一条附带运费的完整信息）
     */
    @GetMapping("/productBook/list")
    @CrossOrigin(origins = "*")
    public Map listProductBook(String isbn, Long queueId) {
        Map result = new HashMap();
        try {
            ErpGoodsOrderQueue queue = erpGoodsOrderQueueService.getById(queueId);
            if (queue == null) {
                result.put("code", "400");
                result.put("msg", "队列记录不存在");
                return result;
            }
            ErpGoodsOrder order = erpGoodsOrderService.selectById(queue.getErpGoodsOrderId());
            if (order == null) {
                result.put("code", "400");
                result.put("msg", "订单不存在");
                return result;
            }

            // 调进销存查询商品
            String productRes = InterfaceUtils.getInterface(
                    UrlUtil.getNewWarehouse(),
                    "/api/product_book/list?page=1&page_size=100&keyword=" + isbn
            );
            Map productResMap = JsonUtil.transferToObj(productRes, Map.class);
            if (productResMap == null || !"200".equals(String.valueOf(productResMap.get("code")))) {
                result.put("code", "400");
                result.put("msg", "查询商品失败: " + (productResMap != null ? productResMap.get("msg") : "无响应"));
                return result;
            }

            Map dataMap = (Map) productResMap.get("data");
            List goodsList = (List) dataMap.get("list");
            if (goodsList == null || goodsList.isEmpty()) {
                result.put("code", "400");
                result.put("msg", "未查询到相关商品");
                return result;
            }

            String receiverProvince = order.getProvince() != null
                    ? order.getProvince().replace("省", "").replace("市", "").replace("自治区", "")
                    : "";

            List enrichedList = new ArrayList();
            for (Object obj : goodsList) {
                Map item = (Map) obj;
                String aboutId = String.valueOf(item.get("about_id"));
                String productId = String.valueOf(item.get("self_id"));

                // 查商品完整信息
                String fullInfoRes = InterfaceUtils.getInterface(
                        UrlUtil.getNewWarehouse(),
                        "/api/product/full_info?user_id=" + aboutId + "&product_id=" + productId
                );
                Map fullInfoMap = JsonUtil.transferToObj(fullInfoRes, Map.class);

                Map productInfo = new HashMap();
                if (fullInfoMap != null && "200".equals(String.valueOf(fullInfoMap.get("code")))) {
                    productInfo = (Map) fullInfoMap.get("data");
                } else {
                    productInfo.put("id", item.get("self_id"));
                    productInfo.put("barcode", item.get("isbn"));
                    productInfo.put("sale_price", item.get("sale_price"));
                    productInfo.put("product_name", item.get("product_name"));
                    productInfo.put("about_id", item.get("about_id"));
                }

                // 查库存和运费
                String stockRes = InterfaceUtils.getInterface(
                        UrlUtil.getNewWarehouse(),
                        "/api/product/getProductInventory?user_id=" + aboutId + "&product_id=" + productId + "&type=1"
                );
                Map stockResMap = JsonUtil.transferToObj(stockRes, Map.class);

                BigDecimal cost = BigDecimal.ZERO;
                int quantity = 0;
                if (stockResMap != null && "200".equals(String.valueOf(stockResMap.get("code")))) {
                    Map stockData = (Map) stockResMap.get("data");
                    quantity = Integer.parseInt(String.valueOf(stockData.get("quantity")));
                    cost = calcCost(stockData, receiverProvince);
                }

                // 计算手续费
                BigDecimal handlingFee = BigDecimal.ZERO;
                try {
                    Long orderCreatedBy = order.getCreatedBy();
                    if (!String.valueOf(aboutId).equals(String.valueOf(orderCreatedBy))) {
                        // 分销商品才需要手续费
                        SysUser orderUser = sysUserService.selectUserOne(orderCreatedBy);
                        PsiSplitAccountConfig configPlatform = new PsiSplitAccountConfig();

                        PsiEmployees psiEmployees = null;
                        if (orderUser != null) {
                            psiEmployees = psiEmployeesService.selectOneByAboutIdAndPhone(orderCreatedBy, orderUser.getPhonenumber());
                        }

                        if (psiEmployees != null && psiEmployees.getRuleValue() != null) {
                            List ruleValueList = JsonUtil.transferToObj(psiEmployees.getRuleValue(), List.class);
                            for (Object rv : ruleValueList) {
                                Map rvMap = (Map) rv;
                                if ("分润方".equals(String.valueOf(rvMap.get("product_type")))) {
                                    configPlatform.setProductType("分润方");
                                    configPlatform.setRatio(new BigDecimal(String.valueOf(rvMap.get("ratio"))));
                                    configPlatform.setAddAmount(new BigDecimal(String.valueOf(rvMap.get("add_amount"))).multiply(new BigDecimal(100)));
                                }
                            }
                        } else {
                            configPlatform.setProductType("分润方");
                            configPlatform.setRatio(new BigDecimal("0.03"));
                            configPlatform.setAddAmount(new BigDecimal("0.1").multiply(new BigDecimal(100)));
                        }

                        BigDecimal warehousePrice = new BigDecimal(String.valueOf(item.get("sale_price"))).add(cost);
                        handlingFee = warehousePrice.multiply(configPlatform.getRatio())
                                .setScale(0, RoundingMode.CEILING).add(configPlatform.getAddAmount());
                    }
                } catch (Exception ignore) {
                    // 手续费计算异常不影响返回
                }

                Map enriched = new HashMap();
                enriched.put("product_id", item.get("self_id"));
                enriched.put("barcode", item.get("barcode"));
                enriched.put("book_name", item.get("book_name"));
                enriched.put("sale_price", item.get("sale_price"));
                enriched.put("about_id", String.valueOf(item.get("about_id")));
                enriched.put("freight", cost);
                enriched.put("handling_fee", handlingFee);
                enriched.put("quantity", quantity);
                enriched.put("live_image", item.get("live_image"));
                enriched.put("warehouse_name", item.get("warehouse_name"));
                enriched.put("location_name", item.get("location_name"));

                enrichedList.add(enriched);
            }

            result.put("code", "200");
            result.put("data", enrichedList);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", "500");
            result.put("msg", "系统异常: " + e.getMessage());
        }
        return result;
    }

    /**
     * 手动下发
     */
    @PostMapping("/manualDispatch")
    @CrossOrigin(origins = "*")
    public Map manualDispatch(@RequestParam Long queueId, @RequestParam Long productId, @RequestParam String aboutId) {
        Map result = new HashMap();
        try {
            // 1. 查队列和订单
            ErpGoodsOrderQueue queue = erpGoodsOrderQueueService.getById(queueId);
            if (queue == null) {
                result.put("code", "400");
                result.put("msg", "队列记录不存在");
                return result;
            }
            ErpGoodsOrder order = erpGoodsOrderService.selectById(queue.getErpGoodsOrderId());
            if (order == null) {
                result.put("code", "400");
                result.put("msg", "订单不存在");
                return result;
            }

            // 2. 查商品完整信息
            String fullInfoRes = InterfaceUtils.getInterface(
                    UrlUtil.getNewWarehouse(),
                    "/api/product/full_info?user_id=" + aboutId + "&product_id=" + productId
            );
            Map fullInfoMap = JsonUtil.transferToObj(fullInfoRes, Map.class);
            if (fullInfoMap == null || !"200".equals(String.valueOf(fullInfoMap.get("code")))) {
                result.put("code", "400");
                result.put("msg", "查询商品信息失败");
                return result;
            }
            Map psiProduct = (Map) fullInfoMap.get("data");
            psiProduct.put("about_id", String.valueOf(aboutId));

            // 3. 获取库存和运费
            String stockRes = InterfaceUtils.getInterface(
                    UrlUtil.getNewWarehouse(),
                    "/api/product/getProductInventory?user_id=" + aboutId + "&product_id=" + productId + "&type=1"
            );
            Map stockResMap = JsonUtil.transferToObj(stockRes, Map.class);
            if (stockResMap == null || !"200".equals(String.valueOf(stockResMap.get("code")))) {
                result.put("code", "400");
                result.put("msg", "查询库存失败");
                return result;
            }
            Map stockData = (Map) stockResMap.get("data");
            int quantity = Integer.parseInt(String.valueOf(stockData.get("quantity")));
            if (quantity <= 0) {
                result.put("code", "400");
                result.put("msg", "库存不足");
                return result;
            }

            String receiverProvince = order.getProvince() != null
                    ? order.getProvince().replace("省", "").replace("市", "").replace("自治区", "")
                    : "";
            BigDecimal cost = calcCost(stockData, receiverProvince);
            psiProduct.put("cost", cost);

            // 4. 判断是否分销
            Long orderCreatedBy = order.getCreatedBy();
            boolean isDistribution = !aboutId.equals(String.valueOf(orderCreatedBy));
            System.out.println("手动下发 - isDistribution: " + isDistribution + ", aboutId: " + aboutId + ", orderCreatedBy: " + orderCreatedBy);

            // 5. 下单人信息
            SysUser orderUser = sysUserService.selectUserOne(orderCreatedBy);

            // 6. 分账配置
            BigDecimal salePrice = new BigDecimal(String.valueOf(psiProduct.get("sale_price")));
            BigDecimal totalPrice = salePrice.add(cost);
            BigDecimal handlingFeePlatform = BigDecimal.ZERO;
            BigDecimal handlingFeeWarehouse = BigDecimal.ZERO;

            if (isDistribution) {
                // 分账配置
                PsiSplitAccountConfig configWarehouse = new PsiSplitAccountConfig();
                PsiSplitAccountConfig configPlatform = new PsiSplitAccountConfig();

                PsiEmployees psiEmployees = null;
                if (orderUser != null) {
                    psiEmployees = psiEmployeesService.selectOneByAboutIdAndPhone(orderCreatedBy, orderUser.getPhonenumber());
                }

                if (psiEmployees != null && psiEmployees.getRuleValue() != null) {
                    List ruleValueList = JsonUtil.transferToObj(psiEmployees.getRuleValue(), List.class);
                    for (Object rv : ruleValueList) {
                        Map rvMap = (Map) rv;
                        if ("仓库方".equals(String.valueOf(rvMap.get("product_type")))) {
                            configWarehouse.setProductType("仓库方");
                            configWarehouse.setRatio(new BigDecimal(String.valueOf(rvMap.get("ratio"))));
                            configWarehouse.setAddAmount(new BigDecimal(String.valueOf(rvMap.get("add_amount"))).multiply(new BigDecimal(100)));
                        } else if ("分润方".equals(String.valueOf(rvMap.get("product_type")))) {
                            configPlatform.setProductType("分润方");
                            configPlatform.setRatio(new BigDecimal(String.valueOf(rvMap.get("ratio"))));
                            configPlatform.setAddAmount(new BigDecimal(String.valueOf(rvMap.get("add_amount"))).multiply(new BigDecimal(100)));
                        }
                    }
                } else {
                    // 默认配置
                    configWarehouse.setProductType("仓库方");
                    configWarehouse.setRatio(new BigDecimal("0.03"));
                    configWarehouse.setAddAmount(new BigDecimal("0.1").multiply(new BigDecimal(100)));
                    configPlatform.setProductType("分润方");
                    configPlatform.setRatio(new BigDecimal("0.03"));
                    configPlatform.setAddAmount(new BigDecimal("0.1").multiply(new BigDecimal(100)));
                }

                handlingFeePlatform = totalPrice.multiply(configPlatform.getRatio())
                        .setScale(0, RoundingMode.CEILING).add(configPlatform.getAddAmount());
                handlingFeeWarehouse = totalPrice.multiply(configWarehouse.getRatio())
                        .setScale(0, RoundingMode.CEILING).add(configWarehouse.getAddAmount());

                BigDecimal totalWithFee = totalPrice.add(handlingFeePlatform);

                // 校验余额
                BigDecimal balance = orderUser != null && orderUser.getBalance() != null ? orderUser.getBalance() : BigDecimal.ZERO;
                if (balance.compareTo(totalWithFee) < 0) {
                    result.put("code", "400");
                    result.put("msg", "余额不足，当前余额：" + balance.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                            + "元，需要：" + totalWithFee.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP) + "元");
                    return result;
                }

                // ========== 分账资金流转 ==========

                // 获取商品信息（用于日志）
                String goodsId = null;
                try {
                    String itemList = order.getItemList();
                    if (itemList != null) {
                        Map itemMap = JsonUtil.transferToObj(itemList, Map.class);
                        goodsId = String.valueOf(itemMap.get("goodsId"));
                    }
                } catch (Exception ignored) {}

                String businessNo = order.getOrderSn() + "-" + order.getShopErpId() + "-" + (goodsId != null ? goodsId : "");

                // 分账日志基础信息
                Map createSplitAccountDeductionLog = new HashMap();
                createSplitAccountDeductionLog.put("business_no", businessNo);
                String configIdStr = psiEmployees != null && psiEmployees.getSplitAccountConfigId() != null
                        ? String.valueOf(psiEmployees.getSplitAccountConfigId()) : "1";
                String configNameStr = psiEmployees != null && psiEmployees.getRuleName() != null
                        ? psiEmployees.getRuleName() : "默认规则";
                createSplitAccountDeductionLog.put("config_id", configIdStr);
                createSplitAccountDeductionLog.put("config_name", configNameStr);
                createSplitAccountDeductionLog.put("status", "0");

                // 创建PSI库存同步日志
                String logIdDispatch = tShopGoodsPublishedService.savePsiSyncLog(
                        String.valueOf(productId),
                        String.valueOf(aboutId),
                        JsonUtil.transferToJson(order),
                        order.getItemList(),
                        "扣减库存",
                        String.valueOf(aboutId)
                );

                // 创建销售订单
                tShopGoodsPublishedService.createSalesOrder(
                        String.valueOf(order.getId()),
                        order.getOrderSn(),
                        String.valueOf(productId),
                        String.valueOf(salePrice.longValue()),
                        "1",
                        order.getShopErpName(),
                        String.valueOf(order.getShopErpId()),
                        String.valueOf(order.getCreatedBy()),
                        String.valueOf(order.getShopType()),
                        order.getReceiverName(),
                        order.getMobile(),
                        order.getProvince() + "-" + order.getCity() + "-" + order.getCountry() + "-" + order.getTown()
                );

                // 同步库存
                String log = tShopGoodsPublishedService.synchronizeStockNew(
                        String.valueOf(productId),
                        Long.parseLong(aboutId),
                        -1,
                        quantity,
                        order

                );

                tShopGoodsPublishedService.updatePsiSyncLog(logIdDispatch, String.valueOf(aboutId), "1",
                        String.valueOf(quantity - 1), String.valueOf(quantity),
                        "200", "手动下发成功");

                // 获取管理员（平台账号）
                SysUser adminUser = sysUserService.selectUserOne(1L);
                // 获取仓库用户
                SysUser warehouseUser = sysUserService.selectUserOne(Long.parseLong(aboutId));

                // ======= 分账步骤（与 TShopGoodsPublishedServiceImpl.distribution 一致） =======

                // 6.1 扣减下单人余额
                JSONObject deductionDetails1 = new JSONObject();
                deductionDetails1.put("erpOrderId", order.getId());
                deductionDetails1.put("psiSplitAccountConfigWarehouse", configWarehouse);
                deductionDetails1.put("psiSplitAccountConfigPlatform", configPlatform);
                deductionDetails1.put("product", psiProduct);
                deductionDetails1.put("handlingFeeWarehouse", handlingFeeWarehouse);
                deductionDetails1.put("handlingFeePlatform", handlingFeePlatform);
                deductionDetails1.put("warehouses", stockData.get("warehouses"));

                deductionDetails1.put("msg", "扣款金额");
                deductionDetails1.put("usePhone", orderUser.getPhonenumber());
                deductionDetails1.put("userId", orderUser.getUserId());
                deductionDetails1.put("handlingFee", handlingFeePlatform);
                createSplitAccountDeductionLog(
                        createSplitAccountDeductionLog,
                        balance,
                        "-" + totalWithFee,
                        orderUser.getBalance().subtract(totalWithFee),
                        orderUser.getUserId(),
                        deductionDetails1
                );
                orderUser.setBalance(orderUser.getBalance().subtract(totalWithFee));
                sysUserService.updateMoney(orderUser);

                // 6.2 平台账号增加分润方手续费（冻结）
                BigDecimal oldFreezeAdmin1 = adminUser.getFreeze();
                adminUser.setFreeze(oldFreezeAdmin1.add(handlingFeePlatform));
                sysUserService.updateMoney(adminUser);
                JSONObject deductionDetails2 = new JSONObject();
                deductionDetails2.put("erpOrderId", order.getId());
                deductionDetails2.put("psiSplitAccountConfigWarehouse", configWarehouse);
                deductionDetails2.put("psiSplitAccountConfigPlatform", configPlatform);
                deductionDetails2.put("product", psiProduct);
                deductionDetails2.put("handlingFeeWarehouse", handlingFeeWarehouse);
                deductionDetails2.put("handlingFeePlatform", handlingFeePlatform);
                deductionDetails2.put("warehouses", stockData.get("warehouses"));
                deductionDetails2.put("msg", "冻结资金增加");
                deductionDetails2.put("usePhone", adminUser.getPhonenumber());
                deductionDetails2.put("userId", adminUser.getUserId());
                deductionDetails2.put("fromUser", orderUser.getPhonenumber());
                deductionDetails2.put("handlingFee", "");
                createSplitAccountDeductionLog(
                        createSplitAccountDeductionLog,
                        oldFreezeAdmin1,
                        String.valueOf(handlingFeePlatform),
                        adminUser.getFreeze(),
                        0L,
                        deductionDetails2
                );

                // 6.3 仓库方增加书价（冻结）
                BigDecimal oldFreezeWarehouseAdd = warehouseUser.getFreeze();
                warehouseUser.setFreeze(oldFreezeWarehouseAdd.add(totalPrice));
                sysUserService.updateMoney(warehouseUser);
                JSONObject deductionDetails3 = new JSONObject();
                deductionDetails3.put("erpOrderId", order.getId());
                deductionDetails3.put("psiSplitAccountConfigWarehouse", configWarehouse);
                deductionDetails3.put("psiSplitAccountConfigPlatform", configPlatform);
                deductionDetails3.put("product", psiProduct);
                deductionDetails3.put("handlingFeeWarehouse", handlingFeeWarehouse);
                deductionDetails3.put("handlingFeePlatform", handlingFeePlatform);
                deductionDetails3.put("warehouses", stockData.get("warehouses"));
                deductionDetails3.put("msg", "冻结资金增加");
                deductionDetails3.put("usePhone", warehouseUser.getPhonenumber());
                deductionDetails3.put("userId", warehouseUser.getUserId());
                deductionDetails3.put("fromUser", orderUser.getPhonenumber());
                deductionDetails3.put("handlingFee", "");
                createSplitAccountDeductionLog(
                        createSplitAccountDeductionLog,
                        oldFreezeWarehouseAdd,
                        String.valueOf(totalPrice),
                        warehouseUser.getFreeze(),
                        warehouseUser.getUserId(),
                        deductionDetails3
                );

                // 6.4 扣除仓库方手续费
                BigDecimal oldFreezeWarehouseSub = warehouseUser.getFreeze();
                warehouseUser.setFreeze(oldFreezeWarehouseSub.subtract(handlingFeeWarehouse));
                sysUserService.updateMoney(warehouseUser);
                JSONObject deductionDetails4 = new JSONObject();
                deductionDetails4.put("erpOrderId", order.getId());
                deductionDetails4.put("psiSplitAccountConfigWarehouse", configWarehouse);
                deductionDetails4.put("psiSplitAccountConfigPlatform", configPlatform);
                deductionDetails4.put("product", psiProduct);
                deductionDetails4.put("handlingFeeWarehouse", handlingFeeWarehouse);
                deductionDetails4.put("handlingFeePlatform", handlingFeePlatform);
                deductionDetails4.put("warehouses", stockData.get("warehouses"));
                deductionDetails4.put("msg", "冻结资金扣除手续费");
                deductionDetails4.put("usePhone", warehouseUser.getPhonenumber());
                deductionDetails4.put("userId", warehouseUser.getUserId());
                deductionDetails4.put("fromUser", "");
                deductionDetails4.put("handlingFee", handlingFeeWarehouse);
                createSplitAccountDeductionLog(
                        createSplitAccountDeductionLog,
                        oldFreezeWarehouseSub,
                        "-" + handlingFeeWarehouse,
                        warehouseUser.getFreeze(),
                        warehouseUser.getUserId(),
                        deductionDetails4
                );

                // 6.5 平台账号增加仓库方手续费（冻结）
                BigDecimal oldFreezeAdmin2 = adminUser.getFreeze();
                adminUser.setFreeze(oldFreezeAdmin2.add(handlingFeeWarehouse));
                sysUserService.updateMoney(adminUser);
                JSONObject deductionDetails5 = new JSONObject();
                deductionDetails5.put("erpOrderId", order.getId());
                deductionDetails5.put("psiSplitAccountConfigWarehouse", configWarehouse);
                deductionDetails5.put("psiSplitAccountConfigPlatform", configPlatform);
                deductionDetails5.put("product", psiProduct);
                deductionDetails5.put("handlingFeeWarehouse", handlingFeeWarehouse);
                deductionDetails5.put("handlingFeePlatform", handlingFeePlatform);
                deductionDetails5.put("warehouses", stockData.get("warehouses"));
                deductionDetails5.put("msg", "冻结资金增加");
                deductionDetails5.put("usePhone", adminUser.getPhonenumber());
                deductionDetails5.put("userId", adminUser.getUserId());
                deductionDetails5.put("fromUser", warehouseUser.getPhonenumber());
                deductionDetails5.put("handlingFee", "");
                createSplitAccountDeductionLog(
                        createSplitAccountDeductionLog,
                        oldFreezeAdmin2,
                        String.valueOf(handlingFeeWarehouse),
                        adminUser.getFreeze(),
                        0L,
                        deductionDetails5
                );

            } else {
                // 非分销：直接下发，不需要分账
                String logIdDispatch2 = tShopGoodsPublishedService.savePsiSyncLog(
                        String.valueOf(productId),
                        String.valueOf(aboutId),
                        JsonUtil.transferToJson(order),
                        order.getItemList(),
                        "扣减库存",
                        String.valueOf(aboutId)
                );

                tShopGoodsPublishedService.createSalesOrder(
                        String.valueOf(order.getId()),
                        order.getOrderSn(),
                        String.valueOf(productId),
                        String.valueOf(salePrice.longValue()),
                        "1",
                        order.getShopErpName(),
                        String.valueOf(order.getShopErpId()),
                        String.valueOf(order.getCreatedBy()),
                        String.valueOf(order.getShopType()),
                        order.getReceiverName(),
                        order.getMobile(),
                        order.getProvince() + "-" + order.getCity() + "-" + order.getCountry() + "-" + order.getTown()
                );

                tShopGoodsPublishedService.updatePsiSyncLog(logIdDispatch2, String.valueOf(aboutId), "1",
                        String.valueOf(quantity - 1), String.valueOf(quantity),
                        "200", "手动下发成功");

                tShopGoodsPublishedService.synchronizeStockNew(
                        String.valueOf(productId),
                        Long.parseLong(aboutId),
                        -1,
                        quantity,
                        order
                );
            }

            // 7. 更新队列状态为成功
            ErpGoodsOrderQueue updateQueue = new ErpGoodsOrderQueue();
            updateQueue.setId(queueId);
            updateQueue.setStatus("1");
            updateQueue.setMsg(isDistribution ? "手动下发成功（分销）" : "手动下发成功（自营）");
            erpGoodsOrderQueueService.update(updateQueue);

            result.put("code", "200");
            result.put("msg", isDistribution ? "下发成功（分销）" : "下发成功（自营）");

        } catch (Exception e) {
            e.printStackTrace();
            // 下发失败也要更新队列状态
            try {
                ErpGoodsOrderQueue failQueue = new ErpGoodsOrderQueue();
                failQueue.setId(queueId);
                failQueue.setStatus("2");
                failQueue.setMsg("手动下发异常: " + e.getMessage());
                erpGoodsOrderQueueService.update(failQueue);
            } catch (Exception ignored) {}
            result.put("code", "500");
            result.put("msg", "下发异常: " + e.getMessage());
        }
        return result;
    }

    /**
     * 计算运费
     */
    private BigDecimal calcCost(Map stockData, String receiverProvince) {
        try {
            List warehousesList = (List) stockData.get("warehouses");
            if (warehousesList == null || warehousesList.isEmpty()) {
                return BigDecimal.ZERO;
            }
            Map warehouses = (Map) warehousesList.get(0);
            Map logistics = (Map) warehouses.get("logistics");
            if (logistics == null) {
                return BigDecimal.ZERO;
            }
            Map shippingRange = JsonUtil.transferToObj(String.valueOf(logistics.get("shippingRange")), Map.class);
            if (shippingRange == null) {
                return BigDecimal.ZERO;
            }
            for (Object key : shippingRange.keySet()) {
                if (String.valueOf(key).contains(receiverProvince)) {
                    List shippingCostList = (List) shippingRange.get(key);
                    BigDecimal headCost = new BigDecimal(String.valueOf(shippingCostList.get(1)))
                            .multiply(new BigDecimal(100));
                    return headCost;
                }
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 记录分账日志（与 TShopGoodsPublishedServiceImpl 一致）
     */
    private void createSplitAccountDeductionLog(Map createSplitAccountDeductionLog, BigDecimal totalAmount,
                                                String deductionAmount, BigDecimal remainingAmount,
                                                Long createdBy, JSONObject deductionDetails) {
        if (createdBy == null) {
            createdBy = 0L;
        }
        if (createdBy == 1) {
            createdBy = 0L;
        }
        BigDecimal totalAmountNew = BigDecimal.ZERO;
        if (totalAmount != null) {
            totalAmountNew = totalAmount.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        }
        createSplitAccountDeductionLog.put("total_amount", totalAmountNew);
        BigDecimal deductionDec;
        try {
            deductionDec = new BigDecimal(deductionAmount).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            deductionDec = BigDecimal.ZERO;
        }
        createSplitAccountDeductionLog.put("deduction_amount", deductionDec);
        createSplitAccountDeductionLog.put("remaining_amount",
                remainingAmount.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
        createSplitAccountDeductionLog.put("created_by", createdBy);
        createSplitAccountDeductionLog.put("deduction_details", deductionDetails.toString());
        String res = InterfaceUtils.postForm(UrlUtil.getNewWarehouse(),
                "/api/split-account-deduction-log/create", createSplitAccountDeductionLog);
        System.out.println("分账日志记录结果: " + res);
    }

}
