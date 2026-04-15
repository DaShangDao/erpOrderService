package com.order.main.mapper;

import com.order.main.entity.SynchronizationShopLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SynchronizationShopLogMapper {

    /**
     * 根据ID查询
     */
    SynchronizationShopLog selectById(Long id);

    /**
     * 根据商品ID查询
     */
    List<SynchronizationShopLog> getListByGoodsId(Long goodsId);

    /**
     * 根据店铺ID查询
     */
    List<SynchronizationShopLog> getListByShopId(Long shopId);

    /**
     * 根据ERP订单ID查询
     */
    List<SynchronizationShopLog> getListByErpOrderId(Long erpOrderId);

    /**
     * 分页查询列表
     */
    List<SynchronizationShopLog> selectPageList(SynchronizationShopLog log);

    /**
     * 查询总记录数
     */
    int count(SynchronizationShopLog log);

    /**
     * 条件查询列表
     */
    List<SynchronizationShopLog> selectList(SynchronizationShopLog query);

    /**
     * 新增
     */
    int insert(SynchronizationShopLog log);

    /**
     * 批量新增
     */
    int insertBatch(@Param("list") List<SynchronizationShopLog> list);

    /**
     * 更新
     */
    int update(SynchronizationShopLog log);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 批量删除
     */
    int deleteBatch(@Param("ids") List<Long> ids);
}