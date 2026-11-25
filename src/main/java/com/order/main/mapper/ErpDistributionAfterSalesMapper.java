package com.order.main.mapper;

import com.order.main.entity.ErpDistributionAfterSales;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
@DS("slave")
public interface ErpDistributionAfterSalesMapper {

    int insert(ErpDistributionAfterSales record);

    ErpDistributionAfterSales selectById(@Param("id") Long id);

    List<ErpDistributionAfterSales> listByDistOrderId(@Param("distOrderId") Long distOrderId);

    /** 进行中的售后（0/1） */
    ErpDistributionAfterSales selectActiveByDistOrderId(@Param("distOrderId") Long distOrderId);

    int updateById(ErpDistributionAfterSales record);

    /** 超时待处理（status 0/1 且 update_time < deadline） */
    List<ErpDistributionAfterSales> listTimeout(@Param("deadline") Date deadline);

    /** 仓库方待处理售后（status 0 待仓库确认） */
    List<ErpDistributionAfterSales> listPendingBySeller(@Param("sellerId") Long sellerId);

    /** 该分销单已售后完成（status=3）过的仓库（seller_id 去重） */
    List<Long> listRefundedSellerIds(@Param("distOrderId") Long distOrderId);
}
