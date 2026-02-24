package com.order.main.config;

import com.order.main.service.IErpGoodsOrderService;
import com.pdd.pop.sdk.http.PopHttpClient;
import com.pdd.pop.sdk.message.MessageHandler;
import com.pdd.pop.sdk.message.WsClient;
import com.pdd.pop.sdk.message.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 标记为配置类
public class PddConfig {

    @Value("${pdd.client.id}") // 从配置文件中获取
    private String clientId; // 拼多多开放平台获取

    @Value("${pdd.client.secret}") // 从配置文件中获取
    private String clientSecret; // 拼多多开放平台获取

    @Autowired
    private IErpGoodsOrderService erpGoodsOrderService;


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

                        if(orderType.equals("pdd_trade_TradeConfirmed")                                 // 交易确认消息
                                        || orderType.equals("pdd_trade_TradeSellerShip")                // 卖家发货消息
                                        || orderType.equals("pdd_trade_TradeSuccess")                   // 交易成功消息
                                        || orderType.equals("pdd_refund_RefundCreated")                 // 退款创建消息
                                        || orderType.equals("pdd_refund_RefundAgreeAgreement")          // 同意退款协议消息
                                        || orderType.equals("pdd_refund_RefundClosed")                  // 售后单关闭消息
                                        || orderType.equals("pdd_trade_TradeRiskChanged")               // 订单审核状态变更
                        ){
                            // 获取订单详情信息
                            erpGoodsOrderService.pddOrderPush(message,false);
                        }else if(orderType.equals("pdd_trade_TradeMemoModified")                // 交易备注修改消息
                                || orderType.equals("pdd_trade_BuyerMemoModified")              // 买家备注修改消息
                                || orderType.equals("pdd_refund_RefundBuyerModifyAgreement")    // 买家修改退款协议消息
                                || orderType.equals("pdd_refund_RefundBuyerReturnGoods")        // 买家退货给卖家消息
                                || orderType.equals("pdd_refund_RefundCreateMessage")){         // 发表退款留言消息
                            erpGoodsOrderService.pddOtherMessage(message);
                        } else if(
                                orderType.equals("pdd_goods_GoodsOffShelf")                     // 商品下架消息
                                || orderType.equals("pdd_goods_GoodsOnShelf")                   // 商品上架消息
                                || orderType.equals("pdd_goods_GoodsAdd")                       // 商品新建消息
                                || orderType.equals("pdd_goods_GoodsUpdate")                    // 商品更新消息
                                || orderType.equals("pdd_goods_GoodsDelete")                    // 商品删除消息
                                || orderType.equals("pdd_goods_GoodsCheckReject")               // 商品审核驳回消息
                        ){
                            // 商品审核驳回消息需要进行额外操作
                            if(orderType.equals("pdd_goods_GoodsCheckReject")){
                                // 删除店铺商品
                                erpGoodsOrderService.pddReviewRejected(message);
                            }
                            // 将消息存入redis    key：shopId  value :list<Map>  type   erpShopId  shopId  goodsId
                            erpGoodsOrderService.messageSetRedis(message);
                        }
                    }
                });
    }
}
