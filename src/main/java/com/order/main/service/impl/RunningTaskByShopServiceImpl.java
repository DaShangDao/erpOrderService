package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.RunningTask;
import com.order.main.mapper.RunningTaskByShopMapper;
import com.order.main.service.IRunningTaskByShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 执行的任务Service业务层处理
 *
 * @author yxy
 * @date 2025-08-5
 */
@RequiredArgsConstructor
@Service
public class RunningTaskByShopServiceImpl implements IRunningTaskByShopService {

    private final RunningTaskByShopMapper baseMapper;


    @Override
    @DS("taskDb")
    public RunningTask selectByTrilateralId(String tableName, String trilateralId) {
        return baseMapper.selectByTrilateralId(tableName, trilateralId);
    }

}
