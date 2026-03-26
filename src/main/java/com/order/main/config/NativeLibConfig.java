package com.order.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "native.lib")
@Data
public class NativeLibConfig {

    /**
     * 孔夫子库路径
     */
    private String kfz;

    /**
     * 拼多多库路径
     */
    private String pdd;

    /**
     * 打单库路径
     */
    private String printSimple;

    /**
     * excel库路径
     */
    private String simple;

    /**
     * 闲鱼库路径
     */
    private String xy;

    /**
     * 闲鱼配置文件路径
     */
    private String xyConfig;

    /**
     * 订单日志路径
     */
    private String orderLog;
}