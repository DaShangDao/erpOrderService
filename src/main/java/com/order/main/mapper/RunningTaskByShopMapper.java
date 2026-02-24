package com.order.main.mapper;


import com.order.main.entity.RunningTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 执行的任务Mapper接口
 *
 * @author yxy
 * @date 2025-08-05
 */
@Mapper
public interface RunningTaskByShopMapper {

    RunningTask selectByTrilateralId(@Param("tableName") String tableName,@Param("trilateralId") String trilateralId);

}
