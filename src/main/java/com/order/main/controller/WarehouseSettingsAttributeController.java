package com.order.main.controller;

import com.order.main.entity.WarehouseSettings;
import com.order.main.entity.WarehouseSettingsAttribute;
import com.order.main.service.IWarehouseSettingsAttributeService;
import com.order.main.service.IWarehouseSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/warehouseSettingAttribute")
public class WarehouseSettingsAttributeController {

    private final IWarehouseSettingsAttributeService warehouseSettingsAttributeService;

    @GetMapping("/getList")
    public List<WarehouseSettingsAttribute> getList(WarehouseSettingsAttribute warehouseSettingsAttribute){
        return warehouseSettingsAttributeService.selectList(warehouseSettingsAttribute);
    }

}
