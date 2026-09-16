package com.order.main.service;

import com.order.main.entity.PddGoodsMessage;
import com.order.main.entity.PddOrderMessage;
import com.pdd.pop.sdk.message.model.Message;

import java.util.List;

/**
 * 拼多多消息入库/消费 Service
 *
 * @author yxy
 * @date 2026-08-04
 */
public interface IPddMessageService {

    /**
     * 订单类/备注类消息入库（WebSocket 收到后调用，毫秒级）
     */
    void saveOrderMessage(Message message);

    /**
     * 商品类消息入库（WebSocket 收到后调用，毫秒级）
     */
    void saveGoodsMessage(Message message);

    /**
     * 查询待处理订单消息（最旧优先，最多 limit 条，含 status=0 待处理 与 status=3 待重试）
     */
    List<PddOrderMessage> selectPendingOrderMessages(int limit);

    /**
     * 查询待处理商品消息（最旧优先，最多 limit 条）
     */
    List<PddGoodsMessage> selectPendingGoodsMessages(int limit);

    /**
     * 更新订单消息状态
     */
    void updateOrderMessageStatus(Long id, int status, int retryCount);

    /**
     * 删除商品消息（先删后处理）
     */
    void deleteGoodsMessage(Long id);

    /**
     * 将Message对象重新组装（从数据库记录还原）
     */
    Message buildMessage(String type, Long mallId, String content);
}
