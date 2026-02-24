package com.order.main.service;

import com.order.main.entity.RunningTask;

/**
 * 执行的任务Service接口
 *
 * @author yxy
 */
public interface IRunningTaskByShopService {


    /**
     * 根据平台商品id查询商品信息
     * @param tableName
     * @param trilateralId
     * @return
     */
    RunningTask selectByTrilateralId(String tableName, String trilateralId);

}
