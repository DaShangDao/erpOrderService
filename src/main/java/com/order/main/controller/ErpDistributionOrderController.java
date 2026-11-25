package com.order.main.controller;

import com.order.main.entity.ErpDistributionOrder;
import com.order.main.entity.ErpDistributionOrderLog;
import com.order.main.service.IErpDistributionOrderLogService;
import com.order.main.service.IErpDistributionOrderService;
import com.order.main.service.ISysUserService;
import com.order.main.service.TShopGoodsPublishedService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 分销订单（分销缓冲层）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/erpDistributionOrder")
@CrossOrigin(origins = "*")
public class ErpDistributionOrderController {

    private final IErpDistributionOrderService erpDistributionOrderService;
    private final IErpDistributionOrderLogService erpDistributionOrderLogService;
    private final ISysUserService sysUserService;
    private final com.order.main.service.IErpDistributionAfterSalesService afterSalesService;
    private final TShopGoodsPublishedService tShopGoodsPublishedService;

    /**
     * 手动重新下发
     */
    @PostMapping("/retry")
    public Map retry(@RequestParam("id") Long id,
                     @RequestParam(value = "operatorId", required = false) Long operatorId) {
        Map result = new HashMap();
        try {
            result = tShopGoodsPublishedService.retryDistributionOrder(id, operatorId);
        } catch (Exception e) {
            result.put("code", "500");
            result.put("msg", "重新下发异常：" + e.getMessage());
        }
        return result;
    }

    /**
     * 手动下发：按 ISBN/书名查询可选商品
     */
    @GetMapping("/productBook/list")
    public Map listProducts(@RequestParam("id") Long id,
                            @RequestParam(value = "isbn", required = false) String isbn) {
        return tShopGoodsPublishedService.listDistributionProducts(id, isbn);
    }

    /**
     * 手动下发：选商品后重新算钱、分账、下发 PSI（复用当前 ERP 分销订单）
     */
    @PostMapping("/dispatch")
    public Map dispatch(@RequestParam("id") Long id,
                        @RequestParam("productId") Long productId,
                        @RequestParam("aboutId") String aboutId,
                        @RequestParam(value = "operatorId", required = false) Long operatorId) {
        Map result = new HashMap();
        try {
            result = tShopGoodsPublishedService.dispatchDistributionOrder(id, productId, Long.parseLong(aboutId), operatorId);
        } catch (Exception e) {
            result.put("code", "500");
            result.put("msg", "手动下发异常：" + e.getMessage());
        }
        return result;
    }

    /**
     * 查询单条
     */
    @GetMapping("/get")
    public Map get(@RequestParam("id") Long id) {
        Map result = new HashMap();
        ErpDistributionOrder order = erpDistributionOrderService.getById(id);
        if (order == null) {
            result.put("code", "500");
            result.put("msg", "ERP分销订单不存在");
            return result;
        }
        result.put("code", "200");
        result.put("data", order);
        return result;
    }

    /**
     * 按 ERP 分销单号查询（仓库方打单时用平台单号反查）
     */
    @GetMapping("/getByNo")
    public Map getByNo(@RequestParam("orderNo") String orderNo) {
        Map result = new HashMap();
        ErpDistributionOrder order = erpDistributionOrderService.getByOrderNo(orderNo);
        if (order == null) {
            result.put("code", "500");
            result.put("msg", "ERP分销订单不存在");
            return result;
        }
        result.put("code", "200");
        result.put("data", order);
        return result;
    }

    /**
     * 操作日志（时间线）
     */

    // ==================== 售后 ====================

    @PostMapping("/afterSales/handlePending")
    public Map handlePending(@RequestParam("id") Long id,
                             @RequestParam("sellerId") Long sellerId,
                             @RequestParam(value = "rollbackStock", required = false, defaultValue = "false") Boolean rollbackStock) {
        return afterSalesService.handlePendingRefund(id, sellerId, Boolean.TRUE.equals(rollbackStock));
    }

    @PostMapping("/afterSales/apply")
    public Map applyAfterSales(@RequestParam("id") Long id,
                               @RequestParam("ownerId") Long ownerId,
                               @RequestParam(value = "reason", required = false) String reason) {
        return afterSalesService.apply(id, ownerId, reason);
    }

    @GetMapping("/afterSales/pendingList")
    public Map pendingAfterSales(@RequestParam("sellerId") Long sellerId) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", afterSalesService.listPendingBySeller(sellerId));
        return res;
    }

    @PostMapping("/afterSales/warehouseConfirm")
    public Map warehouseConfirm(@RequestParam("id") Long id,
                                @RequestParam("sellerId") Long sellerId,
                                @RequestParam("refundAmount") Long refundAmount,
                                @RequestParam(value = "remark", required = false) String remark) {
        return afterSalesService.warehouseConfirm(id, sellerId, refundAmount, remark);
    }

    @PostMapping("/afterSales/distributorConfirm")
    public Map distributorConfirm(@RequestParam("id") Long id,
                                  @RequestParam("ownerId") Long ownerId,
                                  @RequestParam(value = "remark", required = false) String remark) {
        return afterSalesService.distributorConfirm(id, ownerId, remark);
    }

    @PostMapping("/afterSales/distributorReject")
    public Map distributorReject(@RequestParam("id") Long id,
                                 @RequestParam("ownerId") Long ownerId,
                                 @RequestParam(value = "remark", required = false) String remark) {
        return afterSalesService.distributorReject(id, ownerId, remark);
    }

    @GetMapping("/splitLogs")
    public Map splitLogs(@RequestParam("id") Long id) {
        return afterSalesService.listSplitLogs(id);
    }

    @PostMapping("/afterSales/warehouseApply")
    public Map afterSalesWarehouseApply(@RequestParam("orderNo") String orderNo,
                                        @RequestParam("sellerId") Long sellerId,
                                        @RequestParam("reason") String reason,
                                        @RequestParam(value = "rollback", required = false, defaultValue = "0") String rollback,
                                        @RequestParam(value = "outboundId", required = false, defaultValue = "0") Long outboundId) {
        boolean rb = "1".equals(rollback) || "true".equalsIgnoreCase(rollback);
        return afterSalesService.warehouseApply(orderNo, sellerId, reason, rb, outboundId);
    }

    @PostMapping("/afterSales/cancel")
    public Map cancelAfterSales(@RequestParam("id") Long id,
                                @RequestParam("ownerId") Long ownerId,
                                @RequestParam(value = "remark", required = false) String remark) {
        return afterSalesService.cancel(id, ownerId, remark);
    }

    @GetMapping("/afterSales/list")
    public Map afterSalesList(@RequestParam("distOrderId") Long distOrderId) {
        Map result = new HashMap();
        result.put("code", "200");
        result.put("data", afterSalesService.listByDistOrderId(distOrderId));
        return result;
    }

    @GetMapping("/afterSales/detail")
    public Map afterSalesDetail(@RequestParam("id") Long id) {
        Map result = new HashMap();
        result.put("code", "200");
        result.put("data", afterSalesService.getById(id));
        return result;
    }

    @GetMapping("/logs")
    public Map logs(@RequestParam("distOrderId") Long distOrderId) {
        Map result = new HashMap();
        List<ErpDistributionOrderLog> list = erpDistributionOrderLogService.listByDistOrderId(distOrderId);
        if (list != null) {
            for (ErpDistributionOrderLog entry : list) {
                if (entry.getOperatorId() != null && entry.getOperatorId() > 0) {
                    try {
                        com.order.main.entity.SysUser operator = sysUserService.selectUserOne(entry.getOperatorId());
                        if (operator != null) {
                            entry.setOperatorPhone(operator.getPhonenumber());
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        result.put("code", "200");
        result.put("data", list);
        return result;
    }

    /**
     * 按平台订单查询
     */
    @GetMapping("/listBySourceOrder")
    public Map listBySourceOrder(@RequestParam("sourceOrderId") Long sourceOrderId) {
        Map result = new HashMap();
        List<ErpDistributionOrder> list = erpDistributionOrderService.listBySourceOrderId(sourceOrderId);
        result.put("code", "200");
        result.put("data", list);
        return result;
    }
}
