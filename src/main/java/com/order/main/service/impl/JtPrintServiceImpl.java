package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.CourierLog;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.service.ICourierLogService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IJtPrintService;
import com.order.main.util.DateUtils;
import com.order.main.util.SignatureUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class JtPrintServiceImpl implements IJtPrintService {


    private final IErpGoodsOrderService erpGoodsOrderService;
    private final ICourierLogService courierLogService;

    /**
     * 电子面单绑定
     * @param customerCode  账号
     * @param password      密码
     * @return
     */
    @Override
    public String JtVipCheckCusPwd(String customerCode,String password) {
        // 参数定义
        JSONObject bizContent = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("customerCode",customerCode);
        params.put("digest", SignatureUtils.generateSignature(customerCode,password,"4e5aba3a245e4d9a8e18055c187dc9c1"));
        bizContent.put("bizContent",params);
        return PrintSimpleDllLoader.execteJtApi("JtVipCheckCusPwd","814346259086152064","4e5aba3a245e4d9a8e18055c187dc9c1",bizContent.toString());
    }


}
