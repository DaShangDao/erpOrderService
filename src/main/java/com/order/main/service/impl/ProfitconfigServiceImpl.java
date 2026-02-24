package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.service.IProfitconfigService;
import com.order.main.util.InterfaceUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分润配置Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class ProfitconfigServiceImpl implements IProfitconfigService {

    // 用于存储缓存数据的Map，key为orderType，value为包含百分比和过期时间的缓存对象
    private static final Map<String, CacheItem> configCache = new ConcurrentHashMap<>();

    /**
     * 缓存项内部类
     */
    private static class CacheItem {
        private final int percentage;
        private final long expireTime;

        public CacheItem(int percentage, long expireTime) {
            this.percentage = percentage;
            this.expireTime = expireTime;
        }

        public int getPercentage() {
            return percentage;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 获取分润配置
     * @param orderType 订单类型
     * @return 手续费百分比
     */
    @Override
    public int getProfitconfigList(String orderType) {
        // 1. 首先尝试从内存缓存中获取
        CacheItem cacheItem = configCache.get(orderType);

        if (cacheItem != null && !cacheItem.isExpired()) {
            return cacheItem.getPercentage();
        }

        // 2. 缓存中没有或已过期，调用接口获取
        String url = "/profitconfig/list?pageNum=1&pageSize=10&orderType=" + orderType;
        String result = InterfaceUtils.getInterface("http://146.56.227.42:8089", url);
        JSONObject map = JsonUtil.transferToObj(result, JSONObject.class);
        Map data = (Map) map.get("data");
        List list = (List) data.get("list");
        if(list.isEmpty()){
            return 10;
        }
        Map profit = (Map) list.get(0);
        String configStr = profit.get("config").toString();

        // 第一步：去除外层引号
        if (configStr.startsWith("\"") && configStr.endsWith("\"")) {
            configStr = configStr.substring(1, configStr.length() - 1);
        }

        // 第二步：解析转义字符
        configStr = configStr.replace("\\\"", "\"");
        Map configObj = JsonUtil.transferToObj(configStr, Map.class);
        Map floor1 = (Map) configObj.get("floor1");

        // 获取收取手续费的百分比
        int percentage = (int) floor1.get("value");

        // 3. 将结果存入内存缓存，有效期24小时
        long expireTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000L); // 24小时后过期
        CacheItem newCacheItem = new CacheItem(percentage, expireTime);
        configCache.put(orderType, newCacheItem);

        // 4. 可选：清理过期的缓存项（防止内存泄漏）
        cleanExpiredCache();

        return percentage;
    }

    /**
     * 清理过期的缓存项
     */
    private void cleanExpiredCache() {
        // 定期清理，比如每100次调用清理一次，避免每次都遍历
        if (configCache.size() > 20) { // 简单示例，当缓存项较多时清理
            configCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    /**
     * 清理所有缓存（可选方法，供外部调用）
     */
    public void clearCache() {
        configCache.clear();
    }

    /**
     * 获取缓存大小（用于监控，可选）
     */
    public int getCacheSize() {
        return configCache.size();
    }
}