package com.order.main.scheduler;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.entity.OrderCompanyRetry;
import com.order.main.entity.Shop;
import com.order.main.service.IOrderCompanyRetryService;
import com.order.main.service.IShopService;
import com.order.main.util.PddUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 拼多多发货网络超时重试定时器
 * 每2分钟执行一次：查询重试表中 status=0 的记录，按 shop_id 分组重新发货
 */
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.open.orderCompanyRetry", havingValue = "true")
@Component
public class OrderCompanyRetryScheduler {

    private final IOrderCompanyRetryService orderCompanyRetryService;
    private final IShopService shopService;

    // 是否有任务正在执行
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 每2分钟执行一次
     */
    @Scheduled(cron = "0 0/2 * * * ?", zone = "Asia/Shanghai")
    public void retry() {
        // 检查是否有任务正在执行
        if (!isRunning.compareAndSet(false, true)) {
            System.out.println("[OrderCompanyRetry] 上一次重试任务尚未执行完毕，本次任务跳过执行");
            return;
        }

        try {
            // 查询所有待重试记录
            List<OrderCompanyRetry> pendingList = orderCompanyRetryService.getPendingList();
            if (pendingList == null || pendingList.isEmpty()) {
                return;
            }

            // 按 shop_id 分组
            Map<Long, List<OrderCompanyRetry>> groupByShop = new HashMap<>();
            for (OrderCompanyRetry retry : pendingList) {
                groupByShop.computeIfAbsent(retry.getShopId(), k -> new ArrayList<>()).add(retry);
            }

            System.out.println("[OrderCompanyRetry] 本次待重试店铺数: " + groupByShop.size() + ", 记录数: " + pendingList.size());

            // 遍历每个店铺
            for (Map.Entry<Long, List<OrderCompanyRetry>> entry : groupByShop.entrySet()) {
                Long shopId = entry.getKey();
                List<OrderCompanyRetry> shopRetries = entry.getValue();

                // 获取店铺信息（token）
                Shop shop = null;
                try {
                    shop = shopService.queryById(shopId);
                } catch (Exception e) {
                    System.err.println("[OrderCompanyRetry] 查询店铺失败, shopId=" + shopId + ", err=" + e.getMessage());
                    continue;
                }
                if (shop == null) {
                    System.err.println("[OrderCompanyRetry] 店铺不存在, shopId=" + shopId);
                    continue;
                }

                // 遍历该店铺下的所有待重试记录
                for (OrderCompanyRetry retry : shopRetries) {
                    try {
                        // 重新执行发货
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("order_sn", retry.getOrderSn());
                        jsonObject.put("tracking_number", retry.getTrackingNumber());
                        String json = jsonObject.toString();
                        String result = DllInitializer.executePddOrderSynchronization(
                                PddUtil.CLIENT_ID, PddUtil.CLIENT_SECRET, shop.getToken(),
                                retry.getCompanyName(), json);

                        OrderCompanyRetry update = new OrderCompanyRetry();
                        update.setId(retry.getId());
                        update.setRetryCount(retry.getRetryCount() == null ? 1 : retry.getRetryCount() + 1);
                        update.setLastResult(result);

                        if (result != null && result.contains("网络超时异常")) {
                            // 还是网络超时：保持 status=0，下次继续重试
                            update.setStatus(0);
                            System.out.println("[OrderCompanyRetry] 仍超时，继续重试: id=" + retry.getId() + ", shopId=" + shopId + ", orderSn=" + retry.getOrderSn() + ", retryCount=" + update.getRetryCount());
                        } else {
                            // 其他情况都算成功：status=1，不再重试
                            update.setStatus(1);
                            System.out.println("[OrderCompanyRetry] 重试成功: id=" + retry.getId() + ", shopId=" + shopId + ", orderSn=" + retry.getOrderSn() + ", retryCount=" + update.getRetryCount());
                        }
                        orderCompanyRetryService.updateRetryResult(update);
                    } catch (Exception e) {
                        System.err.println("[OrderCompanyRetry] 重试执行异常: id=" + retry.getId() + ", shopId=" + shopId + ", err=" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            isRunning.set(false);
            System.out.println("[OrderCompanyRetry] 重试任务执行完毕");
        }
    }
}
