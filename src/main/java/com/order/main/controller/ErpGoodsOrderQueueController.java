package com.order.main.controller;

import com.order.main.dll.DllInitializer;
import com.order.main.dll.KfzSimpleDllLoader;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ErpGoodsOrderQueue;
import com.order.main.entity.Shop;
import com.order.main.service.IEmsPrintService;
import com.order.main.service.IErpGoodsOrderQueueService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IShopService;
import com.order.main.util.ClientConstantUtils;
import com.order.main.util.MaskUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
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
        List<ErpGoodsOrder> list = erpGoodsOrderQueueService.getPageList(erpGoodsOrder);
        // 对敏感信息做脱敏处理
        if (list != null) {
            for (ErpGoodsOrder order : list) {
                order.setTown(MaskUtils.maskTown(order.getTown()));
                order.setReceiverName(MaskUtils.maskName(order.getReceiverName()));
                order.setMobile(MaskUtils.maskPhone(order.getMobile()));
            }
        }
        map.put("code","200");
        map.put("list",list);
        map.put("pageNum",erpGoodsOrder.getPageNum());
        map.put("pageSize",erpGoodsOrder.getPageSize());
        map.put("total",erpGoodsOrderQueueService.count(erpGoodsOrder));
        return map;
    }

}
