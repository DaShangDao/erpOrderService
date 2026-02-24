package com.order.main.mapper;

import com.order.main.entity.Logistics;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogisticsMapper {

    /**
     * 根据仓库id查询运费模板
     * @param deptId
     * @return
     */
    Logistics selectLogisticeByDeptId(Long deptId);

}
