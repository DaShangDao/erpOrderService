package com.order.main.entity;

import lombok.Data;

/**
 * 单票打印对象 t_single_print
 *
 * @author yxy
 * @date 2026-03-27
 */
@Data
public class SinglePrint {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * erp创建快递单生成的订单号
     */
    private String orderNo;

    /**
     * 快递单号
     */
    private String mailNo;

    /**
     * 发件人名称
     */
    private String senderName;

    /**
     * 发件人联系方式
     */
    private String senderPhone;

    /**
     * 发货地详细地址
     */
    private String senderAddress;

    /**
     * 收件人名称
     */
    private String receiverName;

    /**
     * 收件人联系方式
     */
    private String receiverPhone;

    /**
     * 收件人详细地址
     */
    private String receiverAddress;

    /**
     * 物品名称
     */
    private String itemName;

    /**
     * 物品数量
     */
    private Integer itemNum;

    /**
     * 物品备注
     */
    private String itemRemark;

    /**
     * 打单账号id
     */
    private Long fastMailId;

    /**
     * 打单账号信息
     */
    private String fastMailText;

    /**
     * 创建人id
     */
    private Long createBy;

    /**
     * 创建时间时间戳
     */
    private Long createAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态 1 正常  2 已回收
     */
    private String status;

    /**
     * 分页参数
     */
    private int pageNum;
    private int pageSize;
}