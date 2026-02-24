package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.RunningTask;
import com.order.main.mapper.RunningTaskMapper;
import com.order.main.service.IRunningTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 执行的任务Service业务层处理
 *
 * @author yxy
 * @date 2026-02-05
 */
@RequiredArgsConstructor
@Service
public class RunningTaskServiceImpl implements IRunningTaskService {

    private final RunningTaskMapper baseMapper;

    @Override
    @DS("taskDb")
    public Boolean insert(RunningTask runningTask) {
        return baseMapper.insert(runningTask) > 0;
    }


    @Override
    @DS("taskDb")
    public Boolean batchInsert(List<RunningTask> list){
        return baseMapper.batchInsert(list) > 0;
    }

}
