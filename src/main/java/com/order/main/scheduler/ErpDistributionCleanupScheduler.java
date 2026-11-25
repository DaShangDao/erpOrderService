package com.order.main.scheduler;

import com.order.main.service.IErpDistributionOrderLogService;
import com.order.main.service.IErpDistributionOrderService;
import com.order.main.service.impl.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ERP 分销订单数据保留任务
 *
 * 只保留最近 N 个月（默认 2 个月）的 ERP 分销订单及其操作日志，
 * 超期且已结束的订单物理删除；status 0/1（在途）不删除，只告警。
 *
 * 每天 03:10 执行，Redis 分布式锁防多实例并发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpDistributionCleanupScheduler {

    private static final String LOCK_KEY = "erp:dist:cleanup:lock";

    /** 可删除状态：已下发 / 下发失败 / 已退款关闭 / 已完成 */
    private static final List<Integer> DELETABLE_STATUS = Arrays.asList(2, 3, 4, 5, 6);

    private final IErpDistributionOrderService orderService;
    private final IErpDistributionOrderLogService logService;
    private final RedisService redisService;

    @Value("${erp.distribution.cleanup.enabled:true}")
    private boolean enabled;

    @Value("${erp.distribution.cleanup.retention-months:2}")
    private int retentionMonths;

    @Value("${erp.distribution.cleanup.batch-size:1000}")
    private int batchSize;

    @Value("${erp.distribution.cleanup.max-batches:200}")
    private int maxBatches;

    @Value("${erp.distribution.cleanup.sleep-ms:200}")
    private long sleepMs;

    @Async
    @Scheduled(cron = "${erp.distribution.cleanup.cron:0 10 3 * * ?}", zone = "Asia/Shanghai")
    public void cleanup() {
        if (!enabled) {
            return;
        }
        Boolean locked = redisService.setIfAbsent(LOCK_KEY, "1", 30, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(locked)) {
            log.info("[分销订单清理] 其他实例正在执行，本次跳过");
            return;
        }

        long start = System.currentTimeMillis();
        int batches = 0;
        int orderCount = 0;
        int logCount = 0;
        int failed = 0;
        try {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -retentionMonths);
            Date cutoff = cal.getTime();

            for (int i = 0; i < maxBatches; i++) {
                List<Long> ids = orderService.listExpiredIds(cutoff, DELETABLE_STATUS, batchSize);
                if (ids == null || ids.isEmpty()) {
                    break;
                }
                try {
                    int deletedLogs = logService.deleteByDistOrderIds(ids);
                    int deletedOrders = orderService.deleteByIds(ids);
                    logCount += deletedLogs;
                    orderCount += deletedOrders;
                    batches++;
                } catch (Exception e) {
                    failed++;
                    log.error("[分销订单清理] 第 {} 批删除失败，停止本次清理", i + 1, e);
                    break;
                }
                if (sleepMs > 0) {
                    Thread.sleep(sleepMs);
                }
            }

            long stuck = orderService.countStuckBefore(cutoff);
            if (stuck > 0) {
                log.warn("[分销订单清理告警] 存在 {} 张超过 {} 个月仍未完成的分销订单，请人工处理", stuck, retentionMonths);
            }
            log.info("[分销订单清理完成] 批次={} 删除订单={} 删除日志={} 失败={} 耗时={}ms",
                    batches, orderCount, logCount, failed, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[分销订单清理] 任务异常", e);
        } finally {
            redisService.delete(LOCK_KEY);
        }
    }
}
