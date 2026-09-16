package com.order.main.scheduler;

import com.order.main.entity.PddGoodsMessage;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IPddMessageService;
import com.pdd.pop.sdk.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 拼多多商品类消息消费定时器
 * 每秒执行一次（上一次执行完才开始计时），每轮最多处理100条
 * 纯中转表：查出后先删除，再处理（存Redis），不重试
 * 开关：scheduler.open.pddGoodsMessage=true 时启用
 *
 * @author yxy
 * @date 2026-08-04
 */
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.open.pddGoodsMessage", havingValue = "true")
@Component
public class PddGoodsMessageScheduler {

    /** 每轮最大处理条数（数据量大，中转性质，100条/轮） */
    private static final int BATCH_SIZE = 100;

    private final IPddMessageService pddMessageService;
    private final IErpGoodsOrderService erpGoodsOrderService;

    /** 防重入标志 */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 1000)
    public void process() {
        // 上一次未执行完则跳过本次
        if (!isRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            List<PddGoodsMessage> messages = pddMessageService.selectPendingGoodsMessages(BATCH_SIZE);
            if (messages == null || messages.isEmpty()) {
                return;
            }
            for (PddGoodsMessage record : messages) {
                // 先删除（防宕机重复消费）
                try {
                    pddMessageService.deleteGoodsMessage(record.getId());
                } catch (Exception e) {
                    System.err.println("删除拼多多商品消息失败, id=" + record.getId() + ", err=" + e.getMessage());
                    // 删除失败直接跳过本条，防止重复处理
                    continue;
                }
                // 再处理（存Redis + 驳回特殊处理）
                try {
                    Message message = pddMessageService.buildMessage(
                            record.getMsgType(), record.getMallId(), record.getContent());
                    // 商品审核驳回消息需要额外删除店铺商品操作
                    if ("pdd_goods_GoodsCheckReject".equals(record.getMsgType())) {
                        erpGoodsOrderService.pddReviewRejected(message);
                    }
                    // 所有商品类消息存入 Redis
                    erpGoodsOrderService.messageSetRedis(message);
                } catch (Exception e) {
                    // 不重试，只打日志（中转性质，丢一条可接受）
                    System.err.println("处理拼多多商品消息异常, id=" + record.getId()
                            + ", type=" + record.getMsgType() + ", err=" + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("拼多多商品消息定时器异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isRunning.set(false);
        }
    }
}
