package com.order.main.service;

import com.order.main.entity.*;
import com.pdd.pop.sdk.message.model.Message;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台订单Service接口
 *
 * @author yxy
 * @date 2025-12-04
 */
public interface IErpGoodsOrderService {

    ErpGoodsOrder selectById(Long id);


    /**
     * 闲鱼订单推送
     * @return
     */
    int xyOrderPush(Shop shop, String orderNo,Boolean manua);

    /**
     * 孔夫子订单方法
     */
    void kfzOrderPush(Shop shop, List orderList,Boolean manua);


    /**
     *  拼多多手动同步订单
     * @param shop
     * @param taskId
     * @param startUpdateTime
     * @param endUpdateTime
     */
    void pddManualOrder(Shop shop,String taskId,String startUpdateTime,String endUpdateTime,Boolean manua);

    /**
     *  孔夫子手动同步订单
     * @param shop
     * @param taskId
     * @param startUpdateTime
     * @param endUpdateTime
     */
    void kfzManualOrder(Shop shop,String taskId,String startUpdateTime,String endUpdateTime,Boolean manua);

    /**
     * 拼多多订单推送
     * @param message
     * @return
     */
    int pddOrderPush(Message message,Boolean manua);

    /**
     * 拼多多其他相关消息
     */
    void pddOtherMessage(Message message);

    /**
     * 拼多多审核驳回消息
     */
    void pddReviewRejected(Message message);

    /**
     *  消息插入redis
     */
    void messageSetRedis(Message message);


    /**
     * 回滚费用
     */
    BigDecimal rollbackPrice(OrderExternalGoods orderExternalGoods, Shop shop);


    /**
     * 库存不足时的操作
     * @param shop              店铺信息
     * @param erpGoodsOrder     订单信息
     * @param zhishuShopGoods   商品信息
     * @param quantity          库存
     * @param log               日志
     * @return
     */
    String externalOrderOperation(Shop shop,ErpGoodsOrder erpGoodsOrder,ZhishuShopGoods zhishuShopGoods,int quantity,String log,WarehouseSettings warehouseSettings,Boolean manua);

    /**
     * 查询符合条件的自营书品
     * @param isbn          isbn
     * @param inventory     库存
     * @param shop          店铺信息
     * @param province      收货地（省）
     * @return
     */
    List<ZhishuShopGoods> selectZhishuShopGoods(String isbn, Long inventory, Shop shop, WarehouseSettings warehouseSettings,ErpGoodsOrder erpGoodsOrder);

    /**
     * 库存同步以及订单下发
     * @param shopGoodsId
     * @param inventory
     * @param oldInventory
     * @param createBy
     * @param type
     */
    String synchronizeStock(String shopGoodsId,int inventory,int oldInventory,String createBy,String type,Long erpOrderId);

    /**
     * 根据订单号查询订单信息
     * @param orderNo
     * @return
     */
    ErpGoodsOrder selectByOrderNo(String orderNo);

    /**
     * 拼多多根据订单号获取订单列表
     * @param orderNo
     * @return
     */
    List<ErpGoodsOrder> selectListByOrderNo(String orderNo);

    /**
     * 根据订单号和商品ID查询订单信息
     * @param orderNo
     * @param goodsId
     * @return
     */
    ErpGoodsOrder selectBoOrderNoAndGoodsId(String orderNo,String goodsId);

    /**
     * 分页查询ERP订单（支持动态条件）
     *
     * @param order 查询条件对象
     * @return 订单列表
     */
    List<ErpGoodsOrder> selectPageList(ErpGoodsOrder order);

    /**
     * 获取ERP订单总数（支持动态条件）
     *
     * @param order 查询条件对象
     * @return 订单总数
     */
    int selectPageCount(ErpGoodsOrder order);

    /**
     * 插入平台订单
     *
     * @param erpGoodsOrder 平台订单
     * @return 结果
     */
    int insert(ErpGoodsOrder erpGoodsOrder);

    /**
     * 更新平台订单
     *
     * @param erpGoodsOrder 平台订单
     * @return 结果
     */
    int update(ErpGoodsOrder erpGoodsOrder);

    /**
     * 真删除平台订单
     *
     * @param id 平台订单ID
     * @return 结果
     */
    int deleteById(Long id);

    /**
     * 假删除平台订单（将is_show设为1）
     *
     * @param id 平台订单ID
     * @return 结果
     */
    int fakeDeleteById(Long id);




    /**
     * 统计今日订单
     *
     * @param id 主键
     * @return 订单
     */
    Integer countById(String id);

    /**
     * 统计所有今日订单
     *
     * @return 订单
     */
    Integer countAll();

    /**
     * 统计本月订单
     *
     * @param id 主键
     * @return 订单
     */
    Integer monthOrderById(String id);

    /**
     * 统计所有本月订单
     *
     * @return 订单
     */
    Integer monthOrderAll();

    /**
     * 统计交易总额
     *
     * @param id 主键
     * @return 订单
     */
    BigDecimal todaySale(String id);

    /**
     * 统计所有交易总额
     *
     * @return 订单
     */
    BigDecimal todaySaleAll();

    /**
     * 统计交易总额
     *
     * @param id 主键
     * @return 订单
     */
    BigDecimal monthSale(String id);

    /**
     * 统计所有交易总额
     *
     * @return 订单
     */
    BigDecimal monthSaleAll();
}