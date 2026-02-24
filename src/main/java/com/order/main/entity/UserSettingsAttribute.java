package com.order.main.entity;

import lombok.Data;

@Data
public class UserSettingsAttribute {

    private Long id;
    /**
     * 设置模板id
     */
    private Long warehouseSettingId;

    /**
     * 规则属性id
     */
    private Long attributeId;

    /**
     * 规则内容
     */
    private String attributeValue;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Long createAt;

    /**
     * 修改人
     */
    private Long updateBy;

    /**
     * 修改时间
     */
    private Long updateAt;
}
