package com.order.main.scheduler;

import com.order.main.service.IErpDistributionAfterSalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 售后 7 天超时自动确认
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AfterSalesTimeoutScheduler {

    private final IErpDistributionAfterSalesService afterSalesService;

    @Async
    @Scheduled(cron = "${erp.after.sales.timeout.cron:0 5 * * * ?}", zone = "Asia/Shanghai")
    public void handle() {
        try {
            int count = afterSalesService.handleTimeout();
            if (count > 0) {
                log.info("售后超时自动处理完成，共 {} 单", count);
            }
        } catch (Exception e) {
            log.error("售后超时自动处理异常", e);
        }
    }
}
