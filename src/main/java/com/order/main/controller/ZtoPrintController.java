package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.service.IZtoPrintService;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 中通接口
 *
 * @author yxy
 * @date 2026-4-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ztoPrint")
public class ZtoPrintController {

    private final IZtoPrintService ztoPrintService;

    /**
     * 电子面单绑定
     * @param expressDeliveryType
     * @param account
     * @param password
     * @param json
     * @return
     */
    @GetMapping("/bindingEaccount")
    public Map bindingEaccount(String expressDeliveryType, String account, String password, String json){
        // 返回值对象定义
        Map dataMap = JsonUtil.transferToObj(json,Map.class);
        String res = "";
        if (expressDeliveryType.equals("ZTO")){
            // 中通
            res = ztoPrintService.bindingEaccount(account,password,dataMap);
        }
        Map result = JsonUtil.transferToObj(res, Map.class);
        return result;
    }
}
