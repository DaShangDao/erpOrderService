package com.order.main.service.impl;

import com.order.main.entity.WarehouseSettingsAttribute;
import com.order.main.mapper.WarehouseSettingsAttributeMapper;
import com.order.main.service.IWarehouseSettingsAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class WarehouseSettingsAttributeServiceImpl implements IWarehouseSettingsAttributeService {

    private final WarehouseSettingsAttributeMapper baseMapper;

    /**
     * 查询规则属性列表
     * @param warehouseSettingsAttribute
     * @return
     */
    @Override
    public List<WarehouseSettingsAttribute> selectList(WarehouseSettingsAttribute warehouseSettingsAttribute){
        return baseMapper.selectList(warehouseSettingsAttribute);
    }

}
