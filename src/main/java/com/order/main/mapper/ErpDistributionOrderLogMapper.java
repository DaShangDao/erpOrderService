package com.order.main.mapper;

import com.order.main.entity.ErpDistributionOrderLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * ERP分销订单操作日志 Mapper（zhishu 库）
 */
@Mapper
public interface ErpDistributionOrderLogMapper {

    int insert(ErpDistributionOrderLog log);

    List<ErpDistributionOrderLog> selectByDistOrderId(@Param("distOrderId") Long distOrderId);

    int deleteByDistOrderIds(@Param("ids") List<Long> distOrderIds);

    /** 查最近一次成功下发（自动/手动）时间，且不晚于 before */
    Date selectLastDispatchTime(@Param("distOrderId") Long distOrderId, @Param("before") Date before);

    /** 清理：删除 create_time 早于 cutoff 的孤立日志（对应订单已不存在） */
    int deleteOrphanBefore(@Param("cutoff") Date cutoff);
}
