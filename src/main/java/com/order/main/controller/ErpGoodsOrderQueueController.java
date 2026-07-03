package com.order.main.controller;

import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ErpGoodsOrderQueue;
import com.order.main.service.IEmsPrintService;
import com.order.main.service.IErpGoodsOrderQueueService;
import com.order.main.service.IErpGoodsOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮政快递Controller
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/erpGoodsOrderQueue")
public class ErpGoodsOrderQueueController {

    private final IErpGoodsOrderQueueService erpGoodsOrderQueueService;



    @GetMapping("/getList")
    @CrossOrigin(origins = "*")  // 允许所有来源访问
    public Map getList(ErpGoodsOrder erpGoodsOrder){
        Map map = new HashMap();
        map.put("code","200");
        map.put("list",erpGoodsOrderQueueService.getPageList(erpGoodsOrder));
        map.put("pageNum",erpGoodsOrder.getPageNum());
        map.put("pageSize",erpGoodsOrder.getPageSize());
        map.put("total",erpGoodsOrderQueueService.count(erpGoodsOrder));
        return map;
    }
}
