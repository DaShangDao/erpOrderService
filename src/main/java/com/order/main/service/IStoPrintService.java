package com.order.main.service;


public interface IStoPrintService {


    /**
     * 面单库存查询
     * @param siteCode  网点编号
     * @param userCode  客户编号
     * @param password  客户密码
     * @return
     */
    String getOrderStock(String siteCode,String userCode,String password);

}
