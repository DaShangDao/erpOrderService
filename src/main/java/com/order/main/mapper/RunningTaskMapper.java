package com.order.main.mapper;

import com.order.main.entity.RunningTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 执行的任务Mapper接口
 *
 * @author yxy
 * @date 2026-02-05
 */
@Mapper
public interface RunningTaskMapper {

    int insert(RunningTask runningTask);

    /**
     * 批量插入运行任务记录（如果需要也可以加上租户忽略）
     * @param list 任务列表
     * @return 影响行数
     */
    
    int batchInsert(List<RunningTask> list);

}
