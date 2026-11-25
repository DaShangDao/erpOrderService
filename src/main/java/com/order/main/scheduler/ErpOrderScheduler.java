package com.order.main.scheduler;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ErpGoodsOrderQueue;
import com.order.main.entity.Shop;
import com.order.main.entity.WarehouseSettings;
import com.order.main.service.IErpGoodsOrderQueueService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IShopService;
import com.order.main.service.TShopGoodsPublishedService;
import com.order.main.util.ClientConstantUtils;
import com.order.main.util.DateUtils;
import com.order.main.util.ThreadPoolUtils;
import com.order.main.util.TokenUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.open.erpOrder", havingValue = "true")
@Component
public class ErpOrderScheduler {

    private final IErpGoodsOrderService erpGoodsOrderService;
    private final IErpGoodsOrderQueueService erpGoodsOrderQueueService;
    private final TShopGoodsPublishedService tShopGoodsPublishedService;
    private final IShopService shopService;
    // 添加一个标志位，表示是否有任务正在执行
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    /**
     * 使用配置文件中的cron表达式执行定时任务
     */
    /** 把当前队列行标记为异常（status=3），避免同一条一直失败把后面订单堵死 */
    private void markQueueFail(ErpGoodsOrder order, String msg) {
        if (order == null || order.getQueueId() == null) {
            return;
        }
        try {
            ErpGoodsOrderQueue queue = new ErpGoodsOrderQueue();
            queue.setId(Long.parseLong(order.getQueueId()));
            queue.setStatus("3");
            queue.setMsg(msg != null && msg.length() > 200 ? msg.substring(0, 200) : msg);
            erpGoodsOrderQueueService.update(queue);
            log.warn("队列行已标记异常 queueId={} msg={}", order.getQueueId(), msg);
        } catch (Exception e) {
            log.error("标记队列异常失败 queueId={}", order.getQueueId(), e);
        }
    }

    @Scheduled(cron = "${scheduler.daily.erpOrder}", zone = "Asia/Shanghai")
    public void erpOrder() {
        // 检查是否有任务正在执行
        if (!isRunning.compareAndSet(false, true)) {
            System.out.println("上一次定时任务尚未执行完毕，本次任务跳过执行");
            return;
        }
        ErpGoodsOrder erpGoodsOrder = null;
        try {
            // 获取未下发的订单
            erpGoodsOrder = erpGoodsOrderQueueService.selectByStatus();

            if (erpGoodsOrder != null){
                Shop shop = shopService.queryById(erpGoodsOrder.getShopErpId());
                if (shop == null) {
                    // 店铺被删除/不存在时 getExpirationTime() 会 NPE，并且会让队头一直失败、后面的订单全部堵住
                    markQueueFail(erpGoodsOrder, "店铺不存在或已删除（shop_erp_id=" + erpGoodsOrder.getShopErpId() + "）");
                    return;
                }

                Date now = new Date();
                boolean expired = (shop.getExpirationTime() != null && shop.getExpirationTime().before(now))
                        || (shop.getShopExpirationTime() != null && shop.getShopExpirationTime().before(now));

                if (expired) {
                    // 处理过期逻辑，比如抛异常或返回提示
                    ErpGoodsOrderQueue erpGoodsOrderQueue = new ErpGoodsOrderQueue();
                    erpGoodsOrderQueue.setId(Long.parseLong(erpGoodsOrder.getQueueId()));
                    erpGoodsOrderQueue.setStatus("3");
                    erpGoodsOrderQueue.setMsg("店铺授权/订阅已过期");
                    erpGoodsOrderQueueService.update(erpGoodsOrderQueue);
                }else{
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
                            tShopGoodsPublishedService.createSalesOrder(erpGoodsOrder,warehouseSettings);
                        }else if(erpGoodsOrder.getOrderType().equals("1")){
                            // 订单完成的操作
                            tShopGoodsPublishedService.orderFinish(erpGoodsOrder);
                        }else if (erpGoodsOrder.getOrderType().equals("2")){
                            // 孔夫子订单
                            if ("2".equals(String.valueOf(erpGoodsOrder.getShopType()))){
                                tShopGoodsPublishedService.orderReturnhKfz(erpGoodsOrder);
                            }else{
                                // 订单退款
                                tShopGoodsPublishedService.orderReturnh(erpGoodsOrder);
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            log.error("定时任务处理订单异常，queueId={} orderSn={}",
                    erpGoodsOrder == null ? null : erpGoodsOrder.getQueueId(),
                    erpGoodsOrder == null ? null : erpGoodsOrder.getOrderSn(), e);
            // 标记当前队列行为异常，避免「毒行」阻塞整个队列
            markQueueFail(erpGoodsOrder, "处理异常：" + e.getMessage());
        } finally {
            // 任务执行完毕，重置标志位
            isRunning.set(false);
            System.out.println("定时任务（推送销售订单库存同步）执行完毕");
        }
    }
}