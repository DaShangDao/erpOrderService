package com.order.main.scheduler;

import com.order.main.entity.PddOrderMessage;
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
 * 拼多多订单类/备注类消息消费定时器
 * 每秒执行一次（上一次执行完才开始计时），每轮最多处理20条，最旧的优先，串行执行
 * 开关：scheduler.open.pddOrderMessage=true 时启用
 *
 * @author yxy
 * @date 2026-08-04
 */
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.open.pddOrderMessage", havingValue = "true")
@Component
public class PddOrderMessageScheduler {

    /** 每轮最大处理条数 */
    private static final int BATCH_SIZE = 5;

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
            List<PddOrderMessage> messages = pddMessageService.selectPendingOrderMessages(BATCH_SIZE);
            if (messages == null || messages.isEmpty()) {
                return;
            }
            for (PddOrderMessage record : messages) {
                try {
                    Message message = pddMessageService.buildMessage(
                            record.getMsgType(), record.getMallId(), record.getContent());
                    // 订单类消息：TradeConfirmed/TradeSellerShip/TradeSuccess/退款类等
                    if (isOrderType(record.getMsgType())) {
                        erpGoodsOrderService.pddOrderPush(message, false);
                    } else {
                        // 备注/留言类消息
                        erpGoodsOrderService.pddOtherMessage(message);
                    }
                    // 处理成功
                    pddMessageService.updateOrderMessageStatus(record.getId(), 2, record.getRetryCount());
                } catch (Exception e) {
                    System.err.println("处理拼多多订单消息失败, id=" + record.getId()
                            + ", type=" + record.getMsgType() + ", err=" + e.getMessage());
                    e.printStackTrace();
                    // 第一次失败：status=3 待重试；重试后仍失败：status=9 终态
                    int retryCount = record.getRetryCount() == null ? 0 : record.getRetryCount();
                    if (retryCount >= 1) {
                        // 已经重试过1次，置终态
                        pddMessageService.updateOrderMessageStatus(record.getId(), 9, retryCount + 1);
                    } else {
                        // 第一次失败，标记待重试
                        pddMessageService.updateOrderMessageStatus(record.getId(), 3, retryCount + 1);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("拼多多订单消息定时器异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 判断是否为订单类消息（8种）
     * 备注/留言类（5种）走 else 分支
     */
    private boolean isOrderType(String type) {
        switch (type) {
            case "pdd_trade_TradeConfirmed":        // 交易确认
            case "pdd_trade_TradeSellerShip":       // 卖家发货
            case "pdd_trade_TradeSuccess":          // 交易成功
            case "pdd_refund_RefundCreated":        // 退款创建
            case "pdd_refund_RefundAgreeAgreement": // 同意退款协议
            case "pdd_refund_RefundClosed":         // 售后单关闭
            case "pdd_trade_TradeRiskChanged":      // 订单审核状态变更
            case "pdd_trade_TradeLogisticsAddressChanged": // 修改交易收货地址
                return true;
            default:
                return false;
        }
    }
}
