package com.order.main.service;

import com.order.main.entity.UserSettingsAttribute;
import java.util.List;

public interface IUserSettingsAttributeService {

    // 新增
    int insert(UserSettingsAttribute entity);

    // 批量新增
    int batchInsert(List<UserSettingsAttribute> list);

    // 根据ID查询
    UserSettingsAttribute selectById(Long id);

    // 根据设置表id 查询列表
    List<UserSettingsAttribute> selectByWarehouseSettingsId(Long warehouseSettingsId);

    // 根据条件查询列表
    List<UserSettingsAttribute> selectList(UserSettingsAttribute condition);

    // 根据ID更新
    int updateById(UserSettingsAttribute entity);

    // 根据ID删除
    int deleteById(Long id);

    // 根据 warehouseSettingId 删除
    int deleteByWarehouseSettingId(Long warehouseSettingId);

    // 批量删除
    int batchDeleteByIds(List<Long> ids);

    // 根据 warehouseSettingId 查询列表
    List<UserSettingsAttribute> selectByWarehouseSettingId(Long warehouseSettingId);

    // 根据 attributeId 查询列表
    List<UserSettingsAttribute> selectByAttributeId(Long attributeId);
}