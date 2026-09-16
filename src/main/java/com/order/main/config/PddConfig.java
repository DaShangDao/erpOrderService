package com.order.main.config;

import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IPddMessageService;
import com.pdd.pop.sdk.http.PopHttpClient;
import com.pdd.pop.sdk.message.MessageHandler;
import com.pdd.pop.sdk.message.WsClient;
import com.pdd.pop.sdk.message.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Configuration // 标记为配置类
public class PddConfig {

    @Value("${pdd.client.id}") // 从配置文件中获取
    private String clientId; // 拼多多开放平台获取

    @Value("${pdd.client.secret}") // 从配置文件中获取
    private String clientSecret; // 拼多多开放平台获取

    @Autowired
    private IErpGoodsOrderService erpGoodsOrderService;

    @Autowired
    private IPddMessageService pddMessageService;

    // 日志记录器缓存（按 mallID）
    private static final ConcurrentHashMap<Long, Logger> MESSAGE_LOGGER_CACHE = new ConcurrentHashMap<>();

    private static Logger getMessageLogger(Long mallID) {
        return MESSAGE_LOGGER_CACHE.computeIfAbsent(mallID, id -> {
            try {
                String logDir = "./pdd_message_logs/";
                Files.createDirectories(Path.of(logDir));
                FileHandler fh = new FileHandler(logDir + id + "_%g.log", 50 * 1024 * 1024, 10, true);
                fh.setFormatter(new Formatter() {
                    @Override
                    public String format(LogRecord record) {
                        return record.getMessage() + System.lineSeparator();
                    }
                });
                Logger logger = Logger.getLogger("pdd-msg-" + id);
                logger.setUseParentHandlers(false);
                logger.addHandler(fh);
                return logger;
            } catch (IOException e) {
                // 日志初始化失败，返回一个空 Logger 静默降级
                Logger logger = Logger.getLogger("pdd-msg-" + id);
                logger.setUseParentHandlers(false);
                return logger;
            }
        });
    }


    /**
     * 创建PopHttpClient对象，并设置相关参数
     *
     * @return PopHttpClient对象
     */
    @Bean // 将PopHttpClient对象注册为Spring Bean
    public PopHttpClient popHttpClient() {
        return new PopHttpClient("https://gw-api.pinduoduo.com/api/router", clientId, clientSecret);
    }

    @Bean
    public WsClient wsClient() {
        return new WsClient(
                "wss://message-api.pinduoduo.com",
                clientId,
                clientSecret,
                new MessageHandler() {
                    @Override
                    public void onMessage(Message message) {
                        // 订单类型
                        String orderType = message.getType();

                        if(isOrderMessage(orderType)){
                            // 订单类 + 备注/留言类消息 → 入库（毫秒级），由定时器异步处理
                            pddMessageService.saveOrderMessage(message);
                        }else if(isGoodsMessage(orderType)){
                            // 商品审核驳回消息需要进行额外操作
                            if(orderType.equals("pdd_goods_GoodsCheckReject")){
                                // 删除店铺商品
                                erpGoodsOrderService.pddReviewRejected(message);
                            }
                            // 将消息存入redis    key：shopId  value :list<Map>  type   erpShopId  shopId  goodsId
                            erpGoodsOrderService.messageSetRedis(message);
                        }else{
                            System.out.println("未知类型："+orderType);
                        }
                    }
                });
    }

    /**
     * 判断是否为订单类/备注留言类消息（8种订单 + 5种备注）
     */
    private boolean isOrderMessage(String orderType) {
        return orderType.equals("pdd_trade_TradeConfirmed")                                 // 交易确认消息
                || orderType.equals("pdd_trade_TradeSellerShip")                // 卖家发货消息
                || orderType.equals("pdd_trade_TradeSuccess")                   // 交易成功消息
                || orderType.equals("pdd_refund_RefundCreated")                 // 退款创建消息
                || orderType.equals("pdd_refund_RefundAgreeAgreement")          // 同意退款协议消息
                || orderType.equals("pdd_refund_RefundClosed")                  // 售后单关闭消息
                || orderType.equals("pdd_trade_TradeRiskChanged")               // 订单审核状态变更
                || orderType.equals("pdd_trade_TradeLogisticsAddressChanged")   // 修改交易收货地址消息
                || orderType.equals("pdd_trade_TradeMemoModified")              // 交易备注修改消息
                || orderType.equals("pdd_trade_BuyerMemoModified")              // 买家备注修改消息
                || orderType.equals("pdd_refund_RefundBuyerModifyAgreement")    // 买家修改退款协议消息
                || orderType.equals("pdd_refund_RefundBuyerReturnGoods")        // 买家退货给卖家消息
                || orderType.equals("pdd_refund_RefundCreateMessage");          // 发表退款留言消息
    }

    /**
     * 判断是否为商品类消息（6种）
     */
    private boolean isGoodsMessage(String orderType) {
        return orderType.equals("pdd_goods_GoodsOffShelf")                     // 商品下架消息
                || orderType.equals("pdd_goods_GoodsOnShelf")                   // 商品上架消息
                || orderType.equals("pdd_goods_GoodsAdd")                       // 商品新建消息
                || orderType.equals("pdd_goods_GoodsUpdate")                    // 商品更新消息
                || orderType.equals("pdd_goods_GoodsDelete")                    // 商品删除消息
                || orderType.equals("pdd_goods_GoodsCheckReject");              // 商品审核驳回消息
    }
}
