package com.order.main.controller;

import com.order.main.dto.PublishGoodsRequest;
import com.order.main.service.TShopGoodsPublishedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 发布商品到 t_shop_goods_published 的Controller（对应 newTaskCallBack 功能）
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/publishGoods")
public class PublishGoodsController {

    private final TShopGoodsPublishedService tShopGoodsPublishedService;

    /**
     * 发布商品到 t_shop_goods_published
     * 先按 erp_shop_id + product_id + trilateral_id 删除旧记录，再按 stock 数量插入多条
     */
    @PostMapping("/add")
    @CrossOrigin(origins = "*")
    public Map<String, Object> add(@RequestBody PublishGoodsRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("publishGoods/add 请求参数: userId={}, productId={}, trilateralId={}, stock={}, shopErpId={}",
                    request.getUserId(), request.getProductId(), request.getTrilateralId(),
                    request.getStock(), request.getShopErpId());

            tShopGoodsPublishedService.publishGoods(
                    request.getUserId(),
                    request.getProductId(),
                    request.getTrilateralId(),
                    request.getStock(),
                    request.getShopErpId()
            );

            result.put("code", 200);
            result.put("msg", "发布成功");
            result.put("data", true);
        } catch (Exception e) {
            log.error("publishGoods/add 失败", e);
            result.put("code", 500);
            result.put("msg", e.getMessage());
            result.put("data", false);
        }
        return result;
    }
}
