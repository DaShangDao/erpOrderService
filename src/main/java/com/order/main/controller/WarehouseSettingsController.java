package com.order.main.controller;

import com.order.main.dto.OrderExternalGoodsDto;
import com.order.main.entity.WarehouseSettings;
import com.order.main.service.IWarehouseSettingsService;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/warehouseSetting")
public class WarehouseSettingsController {

    private final IWarehouseSettingsService warehouseSettingsService;

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public WarehouseSettings getById(@PathVariable Long id) {
        return warehouseSettingsService.getById(id);
    }

    @GetMapping("/getList")
    public Map getList(WarehouseSettings warehouseSettings){
        Map map = new HashMap();
        map.put("data",warehouseSettingsService.getPageList(warehouseSettings));
        map.put("total",warehouseSettingsService.count(warehouseSettings));
        return map;
    }

    /**
     * 新增
     */
    @PostMapping("/add")
    public Boolean save(String warehouseSettingsStr) {
        WarehouseSettings warehouseSettings = JsonUtil.transferToObj(warehouseSettingsStr,WarehouseSettings.class);
        return warehouseSettingsService.save(warehouseSettings);
    }

    /**
     * 更新
     */
    @PostMapping("/edit")
    public Boolean update(String warehouseSettingsStr) {
        WarehouseSettings warehouseSettings = JsonUtil.transferToObj(warehouseSettingsStr,WarehouseSettings.class);
        return warehouseSettingsService.update(warehouseSettings);
    }

    /**
     * 状态
     */
    @PostMapping("/editStatus")
    public Boolean editStatus(String warehouseSettingsStr) {
        WarehouseSettings warehouseSettings = JsonUtil.transferToObj(warehouseSettingsStr,WarehouseSettings.class);
        return warehouseSettingsService.updateStatus(warehouseSettings);
    }

    /**
     * 删除
     */
    @PostMapping("/deleteById/{id}")
    public Boolean delete(@PathVariable Long id) {
        return warehouseSettingsService.deleteById(id);
    }
}

