package com.order.main.service;

import java.util.List;
import java.util.Map;

public interface IProfitconfigService {

    /**
     * 获取分润配置
     * @return
     */
    int getProfitconfigList(String orderType);

    /**
     * 清理所有缓存
     */
    void clearCache();

}
