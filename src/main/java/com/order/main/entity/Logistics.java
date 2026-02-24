package com.order.main.entity;


import lombok.Data;
import java.math.BigDecimal;

/**
 * 物流模板对象 t_logistics
 *
 * @author ruoyi
 * @date 2023-07-04
 */
@Data
public class Logistics {

    /**
     * id
     */
    private Long id;

    /**
     * 物流模板名称
     */
    private String templateName;

    /**
     * 发货地-省
     */
    private String deliveryProvince;

    /**
     * 发货地-市
     */
    private String deliveryCity;

    /**
     * 发货地-区
     */
    private String deliveryArea;

    /**
     * 发货地-详细地址
     */
    private String deliveryAddress;

    /**
     * 计价方式（0按重量 1按标准本数（图书专用） 2按件数 3单独设置运费）
     */
    private String pricingMethod;

    /**
     * 运送方式（0快递 1自提）
     */
    private String shipping;

    /**
     * 首重 首本 首件
     */
    private Double firWbv;

    /**
     * 首费 单位：元
     */
    private BigDecimal firPrice;

    /**
     * 续重 续本 续件
     */
    private Double continueWbv;

    /**
     * 续费 单位：元
     */
    private BigDecimal continuePrice;

    /**
     * 模板状态（0正常 1停用）
     */
    private String status;

    /**
     * 租户编码
     */
    private String tenantId;

    /**
     * 运送范围
     */
    private String shippingRange;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 配送说明
     */
    private String remark;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String phoneNumber;
    /**
     * 详细地址
     */
    private String fullAddress;

}
