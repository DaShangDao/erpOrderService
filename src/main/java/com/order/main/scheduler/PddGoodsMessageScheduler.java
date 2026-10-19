package com.order.main.scheduler;

import com.order.main.entity.PddGoodsMessage;
import com.order.main.entity.Shop;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IPddMessageService;
import com.order.main.service.impl.ShopCacheService;
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
    private final ShopCacheService shopCacheService;

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

                        Shop shop = shopCacheService.getShop(message.getMallID());

                        /**
                         * 分类和图片错误调用接口
                         */
                        String content = message.getContent();
                        if (content.contains("您发布商品的分类与商品信息不匹配")
                                || content.contains("商品分类错误")
                                || content.contains("商品分类有误")
                                || content.contains("商品图片")
                                || content.contains("商品轮播图")
                                || content.contains("图片不清晰、失真、未展示商品主体")
                                || content.contains("您发布的商品中存在SKU的文字、图片等描述不规范的行为")
                                || content.contains("文字、图片、SKU等")
                                || content.contains("文字或图片")
                                || content.contains("标题、图片、sku、详情等区域")
                                || content.contains("请删除商品")
                                || content.contains("请在商品")
                                || content.contains("请将商品发布至正确类目")
                                || content.contains("请重新上传")
                        ){
                            // 调用外部接口

                        }else if (
                                content.contains("您发布的商品/信息涉嫌违法")
                                || content.contains("您发布的商品涉嫌违规")
                                || content.contains("无查询结果或查询信息不一致")
                                || content.contains("涉嫌发布")
                                || content.contains("禁止发布")
                                || content.contains("禁止商家发布")
                        ){
                            // 加入违规词

                        }else if(
                                content.contains("请如实填写")
                                || content.contains("请您准确填写")
                                || content.contains("请您如实填写")
                                || content.contains("请规范商品信息")
                        ){
                            // 根据ISBN在拼多多获取商品信息

                        }

                    }

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
