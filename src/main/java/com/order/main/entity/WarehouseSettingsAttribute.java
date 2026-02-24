package com.order.main.entity;

import lombok.Data;

/**
 * 匹配规则 warehouse_settings_attribute
 *
 * @author yxy
 * @date 2025-12-19
 */
@Data
public class WarehouseSettingsAttribute {

    private Long id;

    /**
     * 规则名称
     */
    private String attributeName;

    /**
     * 备注
     */
    private String remark;
}
