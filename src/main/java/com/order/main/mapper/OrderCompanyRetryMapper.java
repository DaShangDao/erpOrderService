package com.order.main.mapper;

import com.order.main.entity.OrderCompanyRetry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderCompanyRetryMapper {

    /**
     * 新增
     */
    int insert(OrderCompanyRetry entity);

    /**
     * 查询待重试记录（status=0）
     */
    List<OrderCompanyRetry> selectPendingList();

    /**
     * 根据主键查询
     */
    OrderCompanyRetry selectById(Long id);

    /**
     * 更新重试结果（更新 retry_count、status、last_result）
     */
    int updateRetryResult(OrderCompanyRetry entity);
}
