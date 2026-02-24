package com.order.main.service;


import com.order.main.entity.Logistics;

public interface LogisticsService {


    /**
     * 根据仓库id查询运费模板
     * @param deptId
     * @return
     */
    Logistics selectLogisticeByDeptId(Long deptId);
}
