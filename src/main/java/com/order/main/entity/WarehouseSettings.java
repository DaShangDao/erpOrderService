package com.order.main.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 设置对象 warehouse_settings
 *
 * @author yxy
 * @date 2025-12-19
 */
@Data
public class WarehouseSettings {

    /**
     * 
     */
    private Long id;

    /**
     * 设置模板名称
     */
    private String settingName;

    /**
     * 库存同步形式  0 下单减库存   1  支付减库存
     */
    private Long stockSynchronizeType;

    /**
     * 是否自动下发  0 否  1 是
     */
    private Long autoIssue;

    /**
     * 是否开启亏损保护  0 否 1 是
     */
    private Long lossProtection;

    /**
     * 利润下限
     */
    private BigDecimal profitFloor;

    /**
     * 是否启用  0 否 1 是
     */
    private Long status;

    /**
     * 是否删除 0 否 1 是
     */
    private Long delFlag;

    /**
     * 退款后是否自动回退库存  0否 1是
     */
    private Long stockRollback;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间时间戳
     */
    private Long createAt;

    /**
     * 修改人
     */
    private Long updateBy;

    /**
     * 修改时间时间戳
     */
    private Long updateAt;

    /**
     * 分页参数
     */
    private int pageNum;
    private int pageSize;

    /**
     * 店铺id
     */
    private String shopIds;


    /**
     * 规则数据
     */
    private String userSettingsAttributeListStr;
    private List<UserSettingsAttribute> userSettingsAttributeList;
}
