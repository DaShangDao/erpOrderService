package com.order.main.controller;

import com.dtflys.forest.annotation.Get;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;
import com.order.main.service.IEmsPrintService;
import com.order.main.service.IErpGoodsOrderService;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 邮政快递Controller
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/emsPrint")
public class EmsPrintController {

    private final IEmsPrintService emsPrintService;
    private final IErpGoodsOrderService erpGoodsOrderService;




}
