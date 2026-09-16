package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.PddGoodsMessage;
import com.order.main.entity.PddOrderMessage;
import com.order.main.mapper.PddGoodsMessageMapper;
import com.order.main.mapper.PddOrderMessageMapper;
import com.order.main.service.IPddMessageService;
import com.pdd.pop.sdk.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 拼多多消息入库/消费 Service 实现
 *
 * @author yxy
 * @date 2026-08-04
 */
@Service
@RequiredArgsConstructor
public class PddMessageServiceImpl implements IPddMessageService {

    private final PddOrderMessageMapper orderMessageMapper;
    private final PddGoodsMessageMapper goodsMessageMapper;
    private final ShopCacheService shopCacheService;

    @Override
    @DS("master")
    public void saveOrderMessage(Message message) {
        PddOrderMessage entity = new PddOrderMessage();
        entity.setMsgType(message.getType());
        entity.setMallId(message.getMallID());
        entity.setContent(message.getContent());
        entity.setStatus(0);
        entity.setRetryCount(0);
        // 查询店铺ID（走缓存，slave库），记录 shop_id 便于排查
        Long shopId = shopCacheService.getShopId(message.getMallID());
        if (shopId != null) {
            entity.setShopId(shopId);
        }
        orderMessageMapper.insert(entity);
    }

    @Override
    @DS("master")
    public void saveGoodsMessage(Message message) {
        PddGoodsMessage entity = new PddGoodsMessage();
        entity.setMsgType(message.getType());
        entity.setMallId(message.getMallID());
        entity.setContent(message.getContent());
        Long shopId = shopCacheService.getShopId(message.getMallID());
        if (shopId != null) {
            entity.setShopId(shopId);
        }
        goodsMessageMapper.insert(entity);
    }

    @Override
    @DS("master")
    public List<PddOrderMessage> selectPendingOrderMessages(int limit) {
        return orderMessageMapper.selectPending(limit);
    }

    @Override
    @DS("master")
    public List<PddGoodsMessage> selectPendingGoodsMessages(int limit) {
        return goodsMessageMapper.selectPending(limit);
    }

    @Override
    @DS("master")
    public void updateOrderMessageStatus(Long id, int status, int retryCount) {
        PddOrderMessage entity = new PddOrderMessage();
        entity.setId(id);
        entity.setStatus(status);
        entity.setRetryCount(retryCount);
        orderMessageMapper.updateStatus(entity);
    }

    @Override
    @DS("master")
    public void deleteGoodsMessage(Long id) {
        goodsMessageMapper.deleteById(id);
    }

    @Override
    public Message buildMessage(String type, Long mallId, String content) {
        Message message = new Message();
        message.setType(type);
        message.setMallID(mallId);
        message.setContent(content);
        return message;
    }
}
