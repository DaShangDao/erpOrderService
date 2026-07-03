package com.order.main.controller;

import com.dtflys.forest.annotation.Get;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;
import com.order.main.service.IJtPrintService;
import com.order.main.service.IZtoPrintService;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 极兔接口
 *
 * @author yxy
 * @date 2026-5-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/jtPrint")
public class JtPrintController {

    private final IJtPrintService jtPrintService;


    @GetMapping("/jtVipCheckCusPwd")
    public String jtVipCheckCusPwd(String customerCode,String password){
        return jtPrintService.jtVipCheckCusPwd(customerCode,password);
    }

    @GetMapping("/jtEssBalance")
    public String jtEssBalance(String customerCode,String password){
        return jtPrintService.jtEssBalance(customerCode,password);
    }


}
