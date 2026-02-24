package com.order.main.service;

import com.order.main.entity.WarehouseSettingsAttribute;

import java.util.List;

public interface IWarehouseSettingsAttributeService {

    /**
     * 查询规则属性列表
     * @param warehouseSettingsAttribute
     * @return
     */
    List<WarehouseSettingsAttribute> selectList(WarehouseSettingsAttribute warehouseSettingsAttribute);

}
