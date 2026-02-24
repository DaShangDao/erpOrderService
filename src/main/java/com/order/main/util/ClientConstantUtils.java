package com.order.main.util;

import org.springframework.stereotype.Component;

@Component
public final class ClientConstantUtils {

    // 孔夫子appId
    public static final int KFZ_APP_ID = 576;

    // 孔夫子appSecret
    public static final String KFZ_APP_SECRET = "256e10220c5b307f5172b1a49c11467a6cfa8038bbe2a7feccc42231852324f8";

    // 孔夫子接口地址
    public final static String KFZ_URL = "https://open.kongfz.com"; // 正式环境

    // PHP接口地址
    public final static String PHP_URL = "http://118.195.130.47:8990/api"; // 正式环境

    // ERP接口地址
    public final static String ERP_URL = "https://api.buzhiyushu.cn"; // 正式环境
    // public final static String ERP_URL = "http://localhost:8080"; // 本地环境

}
