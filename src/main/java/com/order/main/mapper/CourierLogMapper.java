package com.order.main.mapper;

import com.order.main.entity.CourierLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourierLogMapper {

    /**
     * 根据ID查询
     */
    CourierLog selectById(Long id);

    /**
     * 根据erp订单ID查询
     * @param erpOrderId
     * @return
     */
    List<CourierLog> getListByErpOrderId(Long erpOrderId);

    /**
     * 分页查询列表
     */
    List<CourierLog> selectPageList(CourierLog courierLog);

    /**
     * 查询总记录数
     */
    int count(CourierLog courierLog);

    /**
     * 条件查询列表
     */
    List<CourierLog> selectList(CourierLog query);

    /**
     * 新增
     */
    int insert(CourierLog courierLog);

    /**
     * 更新
     */
    int update(CourierLog courierLog);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 根据快递单号删除
     * @param mailNo
     * @return
     */
    int deleteByMailNo(String mailNo);

    /**
     * 批量删除
     */
    int deleteBatch(@Param("ids") List<Long> ids);
}