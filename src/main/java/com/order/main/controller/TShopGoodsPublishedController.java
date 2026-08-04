package com.order.main.controller;

import com.order.main.service.TShopGoodsPublishedService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tShopGoodsPublished")
public class TShopGoodsPublishedController {

    private final TShopGoodsPublishedService tShopGoodsPublishedService;


    @PostMapping("/deleteByErpShopId")
    public Map deleteByErpShopId(Long erpShopId){
        Map map = new HashMap();
        int delNum = tShopGoodsPublishedService.deleteByErpShopId(erpShopId);
        map.put("code","200");
        map.put("msg","删除成功："+delNum+"条");
        return map;
    }

    @PostMapping("/deleteByTrilateralId")
    public Map deleteByTrilateralId(Long trilateralId){
        Map map = new HashMap();
        int delNum = tShopGoodsPublishedService.deleteByTrilateralId(trilateralId);
        map.put("code","200");
        map.put("msg","删除成功："+delNum+"条");
        return map;
    }

}
