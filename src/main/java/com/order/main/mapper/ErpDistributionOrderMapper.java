package com.order.main.mapper;

import com.order.main.entity.ErpDistributionOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ERP分销订单 Mapper（zhishu 库）
 */
@Mapper
public interface ErpDistributionOrderMapper {

    int insert(ErpDistributionOrder order);

    ErpDistributionOrder selectById(@Param("id") Long id);

    ErpDistributionOrder selectByOrderNo(@Param("orderNo") String orderNo);

    ErpDistributionOrder selectBySourceAndItem(@Param("sourceOrderId") Long sourceOrderId,
                                               @Param("productId") Long productId,
                                               @Param("itemSeq") Integer itemSeq);

    List<ErpDistributionOrder> selectBySourceOrderId(@Param("sourceOrderId") Long sourceOrderId);

    /**
     * 更新状态（null 字段不覆盖）
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("msg") String msg,
                     @Param("failStep") Integer failStep,
                     @Param("psiOrderId") Long psiOrderId,
                     @Param("psiOrderNo") String psiOrderNo,
                     @Param("retryCount") Integer retryCount);

    /**
     * 按来源平台订单批量置状态（仅更新指定原状态的行）
     */
    int updateStatusBySourceOrder(@Param("sourceOrderId") Long sourceOrderId,
                                  @Param("fromStatusList") java.util.List<Integer> fromStatusList,
                                  @Param("status") Integer status,
                                  @Param("msg") String msg);

    int incrementRetry(@Param("id") Long id);

    /**
     * 手动下发：更新商品与金额信息
     */
    int updateDispatchInfo(ErpDistributionOrder order);

    /** 清理：查询 create_time 早于 cutoff 且状态可删的订单ID */
    java.util.List<Long> selectExpiredIds(@Param("cutoff") java.util.Date cutoff,
                                          @Param("statusList") java.util.List<Integer> statusList,
                                          @Param("limit") int limit);

    /** 清理：按ID批量物理删除 */
    int deleteByIds(@Param("ids") java.util.List<Long> ids);

    /** 清理：统计超期未完成的订单数（status 0/1） */
    long countStuckBefore(@Param("cutoff") java.util.Date cutoff);
}
