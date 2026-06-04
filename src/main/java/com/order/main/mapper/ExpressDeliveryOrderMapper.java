// ExpressDeliveryOrderMapper.java
package com.order.main.mapper;

import com.order.main.entity.ExpressDeliveryOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExpressDeliveryOrderMapper {

    /**
     * 根据业务订单号查询
     */
    ExpressDeliveryOrder selectByErpOrderId(String erpOrderId);

    /**
     * 根据快递单号查询
     */
    ExpressDeliveryOrder selectByWaybillNo(String waybillNo);

    /**
     * 分页查询列表
     */
    List<ExpressDeliveryOrder> selectPageList(ExpressDeliveryOrder query);

    /**
     * 查询总记录数
     */
    int count(ExpressDeliveryOrder query);

    /**
     * 条件查询列表
     */
    List<ExpressDeliveryOrder> selectList(ExpressDeliveryOrder query);

    /**
     * 新增
     */
    int insert(ExpressDeliveryOrder entity);

    /**
     * 更新
     */
    int update(ExpressDeliveryOrder entity);

    /**
     * 根据业务订单号删除
     */
    int deleteByErpOrderId(String erpOrderId);

    /**
     * 批量删除
     */
    int deleteBatch(@Param("erpOrderIds") List<String> erpOrderIds);
}