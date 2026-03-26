package com.order.main.entity;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 快递日志对象 courier_log
 *
 * @author yxy
 * @date 2026-03-18
 */
@Data
public class CourierLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * erp订单id
     */
    private Long erpOrderId;

    /**
     * 订单编号
     */
    private String orderSn;

    /**
     * 快递单号
     */
    private String mailNo;

    /**
     * 账号
     */
    private String partnerId;

    /**
     * 联调密码
     */
    private String secret;

    /**
     * 发件人信息
     */
    private String sender;

    /**
     * 收件人信息
     */
    private String receiver;

    /**
     * 商品信息
     */
    private String items;

    /**
     * 订单唯一序列号
     */
    private String orderSerialNo;

    /**
     * 类型
     */
    private String type;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Long createAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 分页参数
     */
    private int pageNum;
    private int pageSize;
}