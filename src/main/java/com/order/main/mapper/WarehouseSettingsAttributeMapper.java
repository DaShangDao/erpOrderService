package com.order.main.mapper;

import com.order.main.entity.WarehouseSettingsAttribute;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WarehouseSettingsAttributeMapper {

    /**
     * 查询规则属性列表
     * @param warehouseSettingsAttribute
     * @return
     */
    List<WarehouseSettingsAttribute> selectList(WarehouseSettingsAttribute warehouseSettingsAttribute);

}
