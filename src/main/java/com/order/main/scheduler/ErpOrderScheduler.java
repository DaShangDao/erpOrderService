package com.order.main.scheduler;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ErpGoodsOrderQueue;
import com.order.main.entity.Shop;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.open.erpOrder", havingValue = "true")
@Component
public class ErpOrderScheduler {
    private final IErpGoodsOrderQueueService erpGoodsOrderQueueService;
    private final TShopGoodsPublishedService tShopGoodsPublishedService;
    // 添加一个标志位，表示是否有任务正在执行
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    /**
     * 使用配置文件中的cron表达式执行定时任务
     */
    @Scheduled(cron = "${scheduler.daily.erpOrder}", zone = "Asia/Shanghai")
    public void erpOrder() {
        // 检查是否有任务正在执行
        if (!isRunning.compareAndSet(false, true)) {
            System.out.println("上一次定时任务尚未执行完毕，本次任务跳过执行");
            return;
        }
        try {
            // 获取未下发的订单
            ErpGoodsOrder erpGoodsOrder = erpGoodsOrderQueueService.selectByStatus();
            if (erpGoodsOrder != null){
                // 下发
                tShopGoodsPublishedService.createSalesOrder(erpGoodsOrder);
            }
        }finally {
            // 任务执行完毕，重置标志位
            isRunning.set(false);
            System.out.println("定时任务（推送销售订单库存同步）执行完毕");
        }
    }
}