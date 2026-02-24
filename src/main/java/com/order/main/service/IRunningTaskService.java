package com.order.main.service;

import com.order.main.entity.RunningTask;
import java.util.List;

/**
 * 执行的任务Service接口
 *
 * @author yxy
 * @date 2026-02-05
 */
public interface IRunningTaskService {


    /**
     * 新增执行的任务
     *
     * @param runningTask 执行的任务
     * @return 是否新增成功
     */
    Boolean insert(RunningTask runningTask);


    Boolean batchInsert(List<RunningTask> list);

}
