package com.order.main.controller;

import com.order.main.dto.OrderExternalGoodsDto;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.OrderExternalGoods;
import com.order.main.entity.Shop;
import com.order.main.service.IErpGoodsOrderAccountsService;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IOrderExternalGoodsService;
import com.order.main.service.IShopService;
import com.order.main.util.DateUtils;
import com.order.main.util.OrderUtils;
import com.sun.jna.platform.mac.SystemB;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单关联商品
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orderExternalGoods")
public class OrderExternalGoodsController {

    private final IOrderExternalGoodsService orderExternalGoodsService;
    private final IErpGoodsOrderService erpGoodsOrderService;
    private final IShopService shopService;

    @GetMapping("/getList")
    public Map getList(OrderExternalGoodsDto orderExternalGoodsDto){
        Map map = new HashMap();
        map.put("data",orderExternalGoodsService.selelctListBydeptUseId(orderExternalGoodsDto));
        map.put("total",orderExternalGoodsService.selelctTotalBydeptUseId(orderExternalGoodsDto));
        return map;
    }

    @PostMapping("/editAfterSales")
    public Map editAfterSales(String id,String erpAfterSalesStatus,String erpAssAddress,String erpAssRemark,String erpAssReason){
        String log = "erp售后订单状态："+OrderUtils.getErpAfterSalesStatusTxt(erpAfterSalesStatus)+";";
        // 延后处理
        ErpGoodsOrder ego = erpGoodsOrderService.selectById(Long.parseLong(id));
        Shop shop = shopService.queryById(ego.getShopErpId());
        ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
        erpGoodsOrder.setId(Long.parseLong(id));
        erpGoodsOrder.setErpAfterSalesStatus(Long.parseLong(erpAfterSalesStatus));
        // 校验售后类型
        if(erpAfterSalesStatus.equals("3")){
            // 售后完成，执行退款
            OrderExternalGoods orderExternalGoods = orderExternalGoodsService.selectByOrderIdAndIsDistribution(erpGoodsOrder.getId(),"0");
            BigDecimal total = erpGoodsOrderService.rollbackPrice(orderExternalGoods,shop);
            log += "退款："+total.divide(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP) + "元;";
        }else if(erpAfterSalesStatus.equals("2")){
            // 待仓库收货
            erpGoodsOrder.setErpAssAddress(erpAssAddress);
        }else if(erpAfterSalesStatus.equals("4")){
            // 拒绝售后
            erpGoodsOrder.setErpAssRemark(erpAssRemark);
            log += "仓库拒绝了售后，拒绝原因："+erpAssRemark+";";
        }else if (erpAfterSalesStatus.equals("5")){
            int length = ego.getErpAssCreateAt().toString().length();
            long threeDaysInMillis = 3L * 24 * 60 * 60;
            if (length == 13){
                threeDaysInMillis = threeDaysInMillis * 1000;
            }
            erpGoodsOrder.setErpAssCreateAt(ego.getErpAssCreateAt() + threeDaysInMillis);
            log += "仓库延后了售后，处理截至时间："+ DateUtils.formatTimestampToGMT8(erpGoodsOrder.getErpAssCreateAt())+";";
        }else if (erpAfterSalesStatus.equals("6")){
            erpGoodsOrder.setErpAssReason(erpAssReason);
            // 再次申请售后
            log += "申请售后理由："+erpAssReason+";";
        }
        if (null == erpGoodsOrder.getErpAssRemark()){
            // 如果没有拒绝理由，则设置为空
            erpGoodsOrder.setErpAssRemark("");
        }
        if (null == erpGoodsOrder.getErpAssAddress() && !erpAfterSalesStatus.equals("3")){
            // 如果没有退货地址并且售后状态不是售后完成
            erpGoodsOrder.setErpAssAddress("");
        }
        erpGoodsOrderService.update(erpGoodsOrder);
        // 获取当前时间时间戳
        long nowTime = DateUtils.parseDateTimeToTimestamp(DateUtils.getTimeByDayOffset(0));
        //记录日志
        Boolean bool = OrderUtils.addToOrderExcelLog(ego.getOrderSn()+"_erpAfterSalesOrder",nowTime+"",log,shop.getShopName(),shop.getId().toString());
        if(!bool){
            //记录日志错误则创建日志
            OrderUtils.createOrderExcelLog(ego.getOrderSn()+"_erpAfterSalesOrder",nowTime+"",log,shop.getShopName(),shop.getId().toString());
        }
        Map resultMap = new HashMap();
        resultMap.put("code",200);
        resultMap.put("msg","OK");
        return resultMap;
    }

    @PostMapping("/editWhetherOutbound")
    public Map editWhetherOutbound(String ids){
        Map map = new HashMap();
        map.put("code",200);

        try{
            String[] idArr = ids.split(",");
            for(String id : idArr){
                OrderExternalGoods orderExternalGoods = new OrderExternalGoods();
                orderExternalGoods.setId(Long.parseLong(id));
                orderExternalGoods.setWhetherOutbound(1L);
                orderExternalGoodsService.update(orderExternalGoods);
            }
            map.put("msg","出库成功");
            return map;
        }catch (Exception e){
            System.out.println("出库异常");
            e.printStackTrace();
        }
        map.put("msg","处理异常，请联系管理员");
        return map;

    }
}
