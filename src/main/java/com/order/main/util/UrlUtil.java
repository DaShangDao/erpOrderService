package com.order.main.util;

import org.springframework.stereotype.Component;

@Component
public final class UrlUtil {

    public static String getNewWarehouse(){
        //     return "http://localhost:9090";
        return "https://psi.api.buzhiyushu.cn";
    }
}
