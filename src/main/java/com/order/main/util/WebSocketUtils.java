package com.order.main.util;

public class WebSocketUtils {

    /**
     * 启动pdd消息推送连接
     * 线上环境需开启连接
     * 其他环境需要注释掉，否则会影响线上
     *
     * @return
     */
    public static Boolean getWsClientConnectionConfig() {
        // return true; // 开启连接
        return false; // 关闭连接
    }
}
