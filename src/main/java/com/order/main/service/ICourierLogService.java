package com.order.main.service;

import com.order.main.entity.CourierLog;
import java.util.List;

public interface ICourierLogService {

    /**
     * 根据ID查询快递日志
     */
    CourierLog getById(Long id);

    /**
     * 根据erp订单di货区快递日志
     * @param erpOrderId
     * @return
     */
    List<CourierLog> getListByErpOrderId(Long erpOrderId);

    /**
     * 分页查询快递日志列表
     */
    List<CourierLog> getPageList(CourierLog courierLog);

    /**
     * 查询总记录数（用于分页）
     */
    int count(CourierLog courierLog);

    /**
     * 查询所有快递日志列表
     */
    List<CourierLog> getList(CourierLog query);

    /**
     * 新增快递日志
     */
    boolean save(CourierLog courierLog);

    /**
     * 更新快递日志
     */
    boolean update(CourierLog courierLog);

    /**
     * 根据ID删除快递日志
     */
    boolean deleteById(Long id);

    /**
     * 根据快递单号删除
     * @param mailNo
     * @return
     */
    boolean deleteByMailNo(String mailNo);

    /**
     * 批量删除快递日志
     */
    boolean deleteBatch(List<Long> ids);
}