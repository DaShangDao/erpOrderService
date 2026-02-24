package com.order.main.mapper;

import com.order.main.entity.WarehouseSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WarehouseSettingsMapper {

    /**
     * 根据ID查询
     */
    WarehouseSettings selectById(Long id);

    /**
     * 分页查询列表
     */
    List<WarehouseSettings> selectPageList(WarehouseSettings warehouseSettings);

    /**
     * 查询总记录数
     */
    int count(WarehouseSettings warehouseSettings);

    /**
     * 条件查询列表
     */
    List<WarehouseSettings> selectList(WarehouseSettings query);

    /**
     * 新增
     */
    int insert(WarehouseSettings warehouseSettings);

    /**
     * 更新
     */
    int update(WarehouseSettings warehouseSettings);

    /**
     *  根据修改人进行批量修改
     * @param warehouseSettings
     * @return
     */
    int updateByUpdateBy(WarehouseSettings warehouseSettings);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 批量删除
     */
    int deleteBatch(@Param("ids") List<Long> ids);
}