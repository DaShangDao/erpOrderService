package com.order.main.service;


/**
 * 极兔
 */
public interface IJtPrintService {

    /**
     * 电子面单绑定
     * @param customerCode  账号
     * @param password      密码
     * @return
     */
    String JtVipCheckCusPwd(String customerCode,String password);
}
