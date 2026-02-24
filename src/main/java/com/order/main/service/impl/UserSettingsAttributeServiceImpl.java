package com.order.main.service.impl;

import com.order.main.entity.UserSettingsAttribute;
import com.order.main.mapper.UserSettingsAttributeMapper;
import com.order.main.service.IUserSettingsAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSettingsAttributeServiceImpl implements IUserSettingsAttributeService {

    private final UserSettingsAttributeMapper baseMapper;

    @Override
    @Transactional
    public int insert(UserSettingsAttribute entity) {
        return baseMapper.insert(entity);
    }

    @Override
    @Transactional
    public int batchInsert(List<UserSettingsAttribute> list) {
        return baseMapper.batchInsert(list);
    }

    @Override
    public UserSettingsAttribute selectById(Long id) {
        return baseMapper.selectById(id);
    }

    // 根据设置表id 查询列表
    @Override
    public List<UserSettingsAttribute> selectByWarehouseSettingsId(Long warehouseSettingsId){
        return baseMapper.selectByWarehouseSettingsId(warehouseSettingsId);
    }

    @Override
    public List<UserSettingsAttribute> selectList(UserSettingsAttribute condition) {
        return baseMapper.selectList(condition);
    }

    @Override
    @Transactional
    public int updateById(UserSettingsAttribute entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    @Transactional
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    // 根据 warehouseSettingId 删除
    @Override
    @Transactional
    public int deleteByWarehouseSettingId(Long warehouseSettingId){
        return baseMapper.deleteByWarehouseSettingId(warehouseSettingId);
    }

    @Override
    @Transactional
    public int batchDeleteByIds(List<Long> ids) {
        return baseMapper.batchDeleteByIds(ids);
    }

    @Override
    public List<UserSettingsAttribute> selectByWarehouseSettingId(Long warehouseSettingId) {
        return baseMapper.selectByWarehouseSettingId(warehouseSettingId);
    }

    @Override
    public List<UserSettingsAttribute> selectByAttributeId(Long attributeId) {
        return baseMapper.selectByAttributeId(attributeId);
    }
}