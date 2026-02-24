package com.order.main.mapper;

import com.order.main.entity.UserSettingsAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserSettingsAttributeMapper {

    // 新增
    int insert(UserSettingsAttribute entity);

    // 批量新增
    int batchInsert(@Param("list") List<UserSettingsAttribute> list);

    // 根据ID查询
    UserSettingsAttribute selectById(Long id);

    // 根据设置表id 查询列表
    List<UserSettingsAttribute> selectByWarehouseSettingsId(Long warehouseSettingsId);

    // 查询列表（动态条件）
    List<UserSettingsAttribute> selectList(UserSettingsAttribute condition);

    // 根据ID更新
    int updateById(UserSettingsAttribute entity);

    // 根据ID删除
    int deleteById(Long id);

    // 根据 warehouseSettingId 删除
    int deleteByWarehouseSettingId(Long warehouseSettingId);

    // 批量删除
    int batchDeleteByIds(@Param("ids") List<Long> ids);

    // 根据 warehouseSettingId 查询
    List<UserSettingsAttribute> selectByWarehouseSettingId(Long warehouseSettingId);

    // 根据 attributeId 查询
    List<UserSettingsAttribute> selectByAttributeId(Long attributeId);
}