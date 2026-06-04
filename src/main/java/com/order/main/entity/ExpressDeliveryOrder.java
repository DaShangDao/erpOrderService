package com.order.main.entity;

import lombok.Data;

import java.util.List;

/**
 * 快递单信息
 */
@Data
public class ExpressDeliveryOrder {

    /**
     * 主键
     */
    private Long id;

    /**
     * 快递类型
     */
    private String type;

    /**
     * 订单id
     */
    private String erpOrderId;

    /**
     * 订单号
     */
    private String logisticsOrderNo;

    /**
     * 快递单号
     */
    private String waybillNo;

    /**
     * 大头笔编码
     */
    private String markDestinationCode;

    /**
     * 大头笔名称
     */
    private String markDestinationName;

    /**
     * 集包地编码
     */
    private String packageCode;

    /**
     * 集包地名称
     */
    private String packageCodeName;

    /**
     * 末
     */
    private String moStr;

    /**
     * 寄件人信息
     */
    private Sender sender;
    private String senderStr;

    /**
     * 收件人信息
     */
    private Receiver receiver;
    private String receiverStr;

    /**
     * 商品信息
     */
    private List<Item> itemList;
    private String itemStr;

    /**
     * 快递账号信息
     */
    private String fastMailStr;

    /**
     * 状态 1 创建成功  2 已回收
     */
    private String status;
}
