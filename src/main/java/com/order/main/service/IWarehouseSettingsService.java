package com.order.main.service;

import com.order.main.entity.WarehouseSettings;
import java.util.List;
import java.util.Map;

public interface IWarehouseSettingsService {

    /**
     * 根据ID查询仓库设置
     */
    WarehouseSettings getById(Long id);

    /**
     * 分页查询仓库设置列表
     */
    List<WarehouseSettings> getPageList(WarehouseSettings warehouseSettings);

    /**
     * 查询总记录数（用于分页）
     */
    int count(WarehouseSettings warehouseSettings);

    /**
     * 查询所有仓库设置列表
     */
    List<WarehouseSettings> getList(WarehouseSettings query);

    /**
     * 新增仓库设置
     */
    boolean save(WarehouseSettings warehouseSettings);

    /**
     * 更新仓库设置
     */
    boolean update(WarehouseSettings warehouseSettings);

    /**
     * 更新规则状态
     * @param warehouseSettings
     * @return
     */
    boolean updateStatus(WarehouseSettings warehouseSettings);

    /**
     * 根据ID删除仓库设置
     */
    boolean deleteById(Long id);

    /**
     * 批量删除仓库设置
     */
    boolean deleteBatch(List<Long> ids);
}