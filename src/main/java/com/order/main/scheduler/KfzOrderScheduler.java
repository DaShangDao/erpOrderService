package com.order.main.scheduler;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.entity.Shop;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IShopService;
import com.order.main.util.*;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.open.kfzOrder", havingValue = "true")
@Component
public class KfzOrderScheduler {

    private final IShopService shopService;
    private final IErpGoodsOrderService erpGoodsOrderService;
    @Autowired
    private TokenUtils tokenUtils;

    // 添加一个标志位，表示是否有任务正在执行
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 使用配置文件中的cron表达式执行定时任务
     */
    @Scheduled(cron = "${scheduler.daily.kfzOrder}", zone = "Asia/Shanghai")
    public void kfzOrder() {
        // 检查是否有任务正在执行
        if (!isRunning.compareAndSet(false, true)) {
            System.out.println("上一次定时任务尚未执行完毕，本次任务跳过执行");
            return;
        }

        try {
            System.out.println("开始执行定时任务");
            List<Long> shopIds = shopService.selectShopIdsList("2");

            if (!shopIds.isEmpty()) {
                // 使用 CountDownLatch 等待所有线程执行完毕
                CountDownLatch latch = new CountDownLatch(shopIds.size());

                // 定义查询开始时间
                String startUpdateTime = DateUtils.getTimeByDayOffset(-30);
                // 定义查询结束时间
                String endUpdateTime = DateUtils.getTimeByDayOffset(0);
                // 获取结束时间的毫秒级时间戳
                Long endUpdatedAt = DateUtils.parseDateTimeToTimestamp(endUpdateTime);

                // 循环店铺
                for (Long shopId : shopIds) {
                    // 每个店铺开启线程
                    ThreadPoolUtils.execute(() -> {
                        try {
                            // 是否刷新token标识符，默认为false
                            boolean isRefreshToken = false;
                            // 获取店铺信息
                            Shop shop = shopService.queryById(shopId);
                            // 定义分页
                            int pageNum = 1;
                            // 定义参数对象
                            JSONObject jsonObject = new JSONObject();
                            // 查询方
                            jsonObject.put("userType", "seller");
                            // 获取上次更新订单时间戳
                            long startUpdatedAt = 0;
                            if (shop.getStartUpdatedAt() != null) {
                                startUpdatedAt = shop.getStartUpdatedAt() + 1000L;
                            }
                            // 更新上次更新订单时间戳
                            shop.setStartUpdatedAt(endUpdatedAt);
                            // 修改店铺的上次更新订单时间戳
                            shopService.updateShopStartUpdatedAt(shop);
                            // 判断是不是第一次同步
                            if (startUpdatedAt != 0) {
                                // 查询开始时间
                                jsonObject.put("startUpdateTime", DateUtils.formatTimestampToGMT8(startUpdatedAt));
                            } else {
                                // 默认查询开始时间
                                jsonObject.put("startUpdateTime", startUpdateTime);
                            }
                            // 查询结束时间参数
                            jsonObject.put("endUpdateTime", endUpdateTime);
                            int runMark = 0;
                            while (true) {
                                // 分页信息
                                jsonObject.put("pageNum", pageNum);
                                // 每页50条
                                jsonObject.put("pageSize", 50);
                                // 转为json字符串
                                String json = jsonObject.toString();
                                // 调用dll 查询订单列表
                                String result = DllInitializer.executeKongfzOrderList(ClientConstantUtils.KFZ_APP_ID, ClientConstantUtils.KFZ_APP_SECRET, shop.getToken(), json);
                                // 转为Map对象
                                Map resultMap = null;
                                try {
                                    resultMap = JsonUtil.transferToObj(result, Map.class);
                                } catch (Exception e) {
                                    System.out.println("解析异常数据-----------：" + result);
                                    runMark++;
                                    if (runMark > 3) {
                                        break;
                                    } else {
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException ex) {
                                            System.out.println("休息异常--------");
                                        }
                                        continue;
                                    }
                                }

                                // 错误信息为空的情况
                                if (null == resultMap.get("errorResponse")) {
                                    // 解析成功信息
                                    Map successResponse = (Map) resultMap.get("successResponse");
                                    // 获取订单列表
                                    List orderList = (List) successResponse.get("list");
                                    // 调用订单流程方法
                                    erpGoodsOrderService.kfzOrderPush(shop, orderList, false);
                                    // 获取总页数
                                    int pages = (int) successResponse.get("pages");
                                    // 如果当前页数大于等于总页数
                                    if (pageNum >= pages) {
                                        // 结束循环
                                        break;
                                    } else {
                                        // 页数++
                                        pageNum++;
                                    }
                                } else {
                                    // 解析错误信息
                                    Map errorResponse = (Map) resultMap.get("errorResponse");
                                    // 定义token过期的错误码
                                    List<Long> tokenErrorCode = new ArrayList<>();
                                    tokenErrorCode.add(1000L);
                                    tokenErrorCode.add(1001L);
                                    tokenErrorCode.add(2000L);
                                    tokenErrorCode.add(2001L);
                                    if (isRefreshToken) {
                                        // 如果 isRefreshToken = true 代表token已经刷新，但依旧未查询到列表信息
                                        break;
                                    } else if (tokenErrorCode.contains(errorResponse.get("code"))) {
                                        // 刷新token
                                        shop.setToken(tokenUtils.refreshToken(shop.getRefreshToken(), shopId));
                                        // 代表token已刷新
                                        isRefreshToken = true;
                                    } else {
                                        // 如果不是token过期的错误码则直接结束循环
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("处理店铺订单异常，shopId: " + shopId);
                            e.printStackTrace();
                        } finally {
                            // 每个线程执行完毕后，计数器减1
                            latch.countDown();
                        }
                    });
                }

                // 等待所有线程执行完毕
                try {
                    latch.await();
                    System.out.println("所有店铺订单处理完成");
                } catch (InterruptedException e) {
                    System.err.println("等待线程执行完成时被中断");
                    Thread.currentThread().interrupt();
                }
            } else {
                System.out.println("未查询到店铺信息,结束此次定时任务");
            }
        } finally {
            // 任务执行完毕，重置标志位
            isRunning.set(false);
            System.out.println("定时任务执行完毕");
        }
    }
}