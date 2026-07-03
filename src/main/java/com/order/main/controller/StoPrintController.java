package com.order.main.controller;

import com.order.main.service.IEmsPrintService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IStoPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 申通快递Controller
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/emsPrint")
public class StoPrintController {


    private final IStoPrintService stoPrintService;


    /**
     * 面单库存查询
     * @param siteCode  网点编号
     * @param userCode  客户编号
     * @param password  账号密码
     * @return
     */
    @GetMapping("/getOrderStock")
    public String getOrderStock(String siteCode,String userCode,String password){
        return stoPrintService.getOrderStock(siteCode,userCode,password);
    }


}
