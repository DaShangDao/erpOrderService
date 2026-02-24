package com.order.main.mapper;

import com.order.main.entity.StockChangeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockChangeLogMapper {


    /**
     * 根据关联id查询操作日志
     * @param aboutId  关联id
     * @return
     */
    List<StockChangeLog> selectByAboutId(Long shopGoodsId,String aboutId);

    /**
     * 插入库存变更记录
     * @param stockChangeLog 库存变更记录实体
     * @return 插入成功返回影响的行数
     */
    int insert(StockChangeLog stockChangeLog);

}