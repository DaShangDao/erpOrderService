package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.service.IStoPrintService;
import com.order.main.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class StoPrintServiceImpl implements IStoPrintService {


    /**
     * 面单库存查询
     * @param siteCode  网点编号
     * @param userCode  客户编号
     * @param password  客户密码
     * @return
     */
    public String getOrderStock(String siteCode,String userCode,String password){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("siteCode",siteCode);
        jsonObject.put("userCode",userCode);
        jsonObject.put("password",password);
        jsonObject.put("timestamp",  System.currentTimeMillis() / 1000);
        return PrintSimpleDllLoader.exceteSTO("BILL_CODE_QUERY_USER_STOCK","CAKDVcNVcjgqoKH","CAKDVcNVcjgqoKH","3ySS5owwn6ENOSespB2c9fROojSAllA0","billcode_open",jsonObject.toString());
    }

}
