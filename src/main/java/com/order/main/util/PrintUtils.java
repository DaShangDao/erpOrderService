package com.order.main.util;

import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

public class PrintUtils {

    private static String yundaAppKey;
    private static String yundaAppSecret;

    @Value("${yunda.appkey}")
    private String yundaAppKeyValue;

    @Value("${yunda.appsecret}")
    private String yundaAppSecretValue;


    @PostConstruct
    public void init(){
        yundaAppKey = yundaAppKeyValue;
        yundaAppSecret = yundaAppSecretValue;
    }

    public static String getYundaAppKey() {
        return yundaAppKey;
    }

    public static void setYundaAppKey(String yundaAppKey) {
        PrintUtils.yundaAppKey = yundaAppKey;
    }

    public static String getYundaAppSecret() {
        return yundaAppSecret;
    }

    public static void setYundaAppSecret(String yundaAppSecret) {
        PrintUtils.yundaAppSecret = yundaAppSecret;
    }
}
