package com.order.main.service.impl;

import com.order.main.entity.UserSettingsAttribute;
import com.order.main.entity.WarehouseSettings;
import com.order.main.mapper.WarehouseSettingsMapper;
import com.order.main.service.IUserSettingsAttributeService;
import com.order.main.service.IWarehouseSettingsService;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseSettingsServiceImpl implements IWarehouseSettingsService {

    private final WarehouseSettingsMapper baseMapper;
    private final IUserSettingsAttributeService userSettingsAttributeService;

    @Override
    public WarehouseSettings getById(Long id) {
        WarehouseSettings warehouseSettings = baseMapper.selectById(id);
        if (warehouseSettings.getProfitFloor() != null){
            warehouseSettings.setProfitFloor(warehouseSettings.getProfitFloor().divide(new BigDecimal("100"),2, RoundingMode.HALF_UP));
        }
        List<UserSettingsAttribute> userSettingsAttributeList = userSettingsAttributeService.selectByWarehouseSettingsId(id);
        warehouseSettings.setUserSettingsAttributeListStr(JsonUtil.transferToJson(userSettingsAttributeList));
        return warehouseSettings;
    }

    @Override
    public List<WarehouseSettings> getPageList(WarehouseSettings warehouseSettings) {
        warehouseSettings.setPageNum((warehouseSettings.getPageNum()-1)*warehouseSettings.getPageSize());
        return baseMapper.selectPageList(warehouseSettings);
    }

    @Override
    public int count(WarehouseSettings warehouseSettings) {
        return baseMapper.count(warehouseSettings);
    }

    @Override
    public List<WarehouseSettings> getList(WarehouseSettings query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(WarehouseSettings warehouseSettings) {
        // 设置创建时间（时间戳，单位：秒）
        long currentTime = System.currentTimeMillis() / 1000;
        warehouseSettings.setCreateAt(currentTime);
        warehouseSettings.setUpdateAt(currentTime);
        warehouseSettings.setDelFlag(0L); // 默认未删除
        warehouseSettings.setStatus(0L); // 默认启用
        if(warehouseSettings.getProfitFloor() != null){
            warehouseSettings.setProfitFloor(warehouseSettings.getProfitFloor().multiply(new BigDecimal("100")));
        }
        Boolean result = baseMapper.insert(warehouseSettings) > 0;
        if(warehouseSettings.getAutoIssue() == 1){
            List UserSettingsAttributeList = JsonUtil.transferToObj(warehouseSettings.getUserSettingsAttributeListStr(),List.class);
            for(Object object : UserSettingsAttributeList){
                Map map = (Map) object;
                UserSettingsAttribute userSettingsAttribute = new UserSettingsAttribute();
                userSettingsAttribute.setWarehouseSettingId(warehouseSettings.getId());
                userSettingsAttribute.setAttributeId(Long.parseLong(map.get("attributeId").toString()));
                userSettingsAttribute.setAttributeValue(map.get("attributeValue").toString());
                userSettingsAttribute.setCreateBy(warehouseSettings.getCreateBy());
                userSettingsAttribute.setCreateAt(currentTime);
                userSettingsAttribute.setUpdateBy(warehouseSettings.getUpdateBy());
                userSettingsAttribute.setUpdateAt(currentTime);
                userSettingsAttributeService.insert(userSettingsAttribute);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WarehouseSettings warehouseSettings) {
        long currentTime = System.currentTimeMillis() / 1000;
        // 设置更新时间
        warehouseSettings.setUpdateAt(currentTime);
        if(warehouseSettings.getProfitFloor() != null){
            warehouseSettings.setProfitFloor(warehouseSettings.getProfitFloor().multiply(new BigDecimal("100")));
        }
        Boolean result = baseMapper.update(warehouseSettings) > 0;
        if(warehouseSettings.getAutoIssue() == 1){
            List UserSettingsAttributeList = JsonUtil.transferToObj(warehouseSettings.getUserSettingsAttributeListStr(),List.class);
            userSettingsAttributeService.deleteByWarehouseSettingId(warehouseSettings.getId());
            for(Object object : UserSettingsAttributeList){
                Map map = (Map) object;
                UserSettingsAttribute userSettingsAttribute = new UserSettingsAttribute();
                userSettingsAttribute.setWarehouseSettingId(warehouseSettings.getId());
                userSettingsAttribute.setAttributeId(Long.parseLong(map.get("attributeId").toString()));
                userSettingsAttribute.setAttributeValue(map.get("attributeValue").toString());
                userSettingsAttribute.setCreateBy(warehouseSettings.getCreateBy());
                userSettingsAttribute.setCreateAt(currentTime);
                userSettingsAttribute.setUpdateBy(warehouseSettings.getUpdateBy());
                userSettingsAttribute.setUpdateAt(currentTime);
                userSettingsAttributeService.insert(userSettingsAttribute);
            }
        }
        return result;
    }

    /**
     * 更新规则状态
     * @param warehouseSettings
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(WarehouseSettings warehouseSettings){
        Long status = warehouseSettings.getStatus() == 1 ? 0L : 1L;
        if(status == 1){
            //根据修改人修改所有当前修改人的所有规则状态为0
            baseMapper.updateByUpdateBy(warehouseSettings);
        }
        warehouseSettings.setStatus(status);
        return baseMapper.update(warehouseSettings) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        userSettingsAttributeService.deleteByWarehouseSettingId(id);
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Long> ids) {
        return baseMapper.deleteBatch(ids) > 0;
    }

    /**
     * 构建查询参数Map（方便Controller层调用）
     */
    public Map<String, Object> buildQueryParams(String settingName, Long stockSynchronizeType,
                                                Long autoIssue, Long status, Integer pageNum, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.hasText(settingName)) {
            params.put("settingName", settingName);
        }
        if (stockSynchronizeType != null) {
            params.put("stockSynchronizeType", stockSynchronizeType);
        }
        if (autoIssue != null) {
            params.put("autoIssue", autoIssue);
        }
        if (status != null) {
            params.put("status", status);
        }
        if (pageNum != null) {
            params.put("pageNum", pageNum);
        }
        if (pageSize != null) {
            params.put("pageSize", pageSize);
        }

        // 默认只查询未删除的数据
        params.put("delFlag", 0L);

        return params;
    }
}