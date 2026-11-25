package com.order.main.service.impl;

import com.order.main.entity.ErpDistributionAfterSales;
import com.order.main.entity.ErpDistributionOrderLog;
import com.order.main.entity.ErpDistributionOrder;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.SysUser;
import com.order.main.mapper.ErpDistributionAfterSalesMapper;
import com.order.main.mapper.ErpGoodsOrderMapper;
import com.order.main.service.IErpDistributionAfterSalesService;
import com.order.main.service.IErpDistributionOrderService;
import com.order.main.service.ISysUserService;
import com.order.main.service.TShopGoodsPublishedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpDistributionAfterSalesServiceImpl implements IErpDistributionAfterSalesService {

    private static final int TIMEOUT_DAYS = 7;

    /** 下发后多少小时内申请售后，才把分销商手续费（platform_fee）一起退回 */
    @org.springframework.beans.factory.annotation.Value("${erp.distribution.after-sales.fee-refund-hours:2}")
    private long feeRefundHours;

    private final ErpDistributionAfterSalesMapper afterSalesMapper;
    private final IErpDistributionOrderService erpDistributionOrderService;
    private final ISysUserService userService;
    private final ErpGoodsOrderMapper erpGoodsOrderMapper;
    private final org.springframework.beans.factory.ObjectProvider<TShopGoodsPublishedService> stockServiceProvider;
    private final com.order.main.service.IErpDistributionOrderLogService logService;

    @Override
    public Map apply(Long distOrderId, Long ownerId, String reason) {
        ErpDistributionOrder dist = erpDistributionOrderService.getById(distOrderId);
        if (dist == null) {
            return fail("分销订单不存在");
        }
        if (!dist.getOwnerId().equals(ownerId)) {
            return fail("只有分销商可以申请售后");
        }
        if (dist.getStatus() == null || (dist.getStatus() != ErpDistributionOrder.STATUS_PSI_OK
                && dist.getStatus() != ErpDistributionOrder.STATUS_FINISHED)) {
            return fail("该订单当前状态不支持申请售后");
        }
        if (afterSalesMapper.selectActiveByDistOrderId(distOrderId) != null) {
            return fail("该订单已有进行中的售后申请");
        }
        ErpDistributionAfterSales as = new ErpDistributionAfterSales();
        as.setDistOrderId(dist.getId());
        as.setDistOrderNo(dist.getOrderNo());
        as.setSourceOrderId(dist.getSourceOrderId());
        as.setSourceOrderSn(dist.getSourceOrderSn());
        as.setOwnerId(dist.getOwnerId());
        as.setSellerId(dist.getSellerId());
        as.setProductId(dist.getProductId());
        as.setProductName(dist.getProductName());
        as.setReason(reason);
        as.setMaxAmount(dist.getBookPrice());
        as.setStatus(ErpDistributionAfterSales.STATUS_WAIT_WAREHOUSE);
        as.setApplyTime(new Date());
        as.setRejectCount(0);
        as.setSourceType(2);

        // 出库未完成：直接自动回滚（除分销方手续费外全额退），回到可重新下发
        // 注意：这条路径也必须落一条「已完成」售后单，否则售后过的仓库不会被排除在重新下发之外
        if (!isOutboundCompleted(dist)) {
            Date now = new Date();
            as.setStatus(ErpDistributionAfterSales.STATUS_DONE);
            as.setRefundAmount(dist.getBookPrice());
            as.setAutoFlag(1);
            as.setWarehouseTime(now);
            as.setDistributorTime(now);
            as.setFinishTime(now);
            as.setWarehouseRemark("出库未完成，系统自动回滚");
            as.setDistributorRemark("系统自动同意");
            afterSalesMapper.insert(as);
            String autoFlow;
            try {
                autoFlow = refund(as, true, true);
            } catch (Exception e) {
                ErpDistributionAfterSales closeAs = new ErpDistributionAfterSales();
                closeAs.setId(as.getId());
                closeAs.setStatus(ErpDistributionAfterSales.STATUS_CLOSED);
                closeAs.setFinishTime(new Date());
                closeAs.setDistributorRemark("自动回滚失败：" + e.getMessage());
                try {
                    afterSalesMapper.updateById(closeAs);
                } catch (Exception ignore) {
                }
                return fail("自动回滚失败：" + (e.getMessage() == null ? "系统异常" : e.getMessage()));
            }
            writeLog(dist, ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                    ErpDistributionOrderLog.ACTION_AS_AUTO_REFUND, dist.getStatus(),
                    ErpDistributionOrder.STATUS_FAIL_UNSPLIT, dist.getOwnerId(), dist.getBookPrice(),
                    ErpDistributionOrderLog.SETTLE_REFUNDED, ErpDistributionOrderLog.OPERATOR_BUYER, ownerId,
                    withFlow("出库未完成，自动回滚（手续费不退），可重新下发", autoFlow));
            return ok("出库未完成，已自动回滚，可直接重新下发", dist.getId());
        }

        as.setAutoFlag(0);
        afterSalesMapper.insert(as);
        writeLog(dist, ErpDistributionOrderLog.CATEGORY_AFTER_SALES, ErpDistributionOrderLog.ACTION_AS_APPLY,
                dist.getStatus(), dist.getStatus(), dist.getOwnerId(), null, null,
                ErpDistributionOrderLog.OPERATOR_BUYER, ownerId, "申请售后：" + (reason == null ? "" : reason));
        return ok("售后申请已提交，等待仓库方确认", as.getId());
    }

    @Override
    public Map warehouseConfirm(Long id, Long sellerId, Long refundAmount, String remark) {
        ErpDistributionAfterSales as = afterSalesMapper.selectById(id);
        if (as == null) {
            return fail("售后单不存在");
        }
        if (!as.getSellerId().equals(sellerId)) {
            return fail("只有仓库方可以确认");
        }
        if (as.getStatus() == null || as.getStatus() != ErpDistributionAfterSales.STATUS_WAIT_WAREHOUSE) {
            return fail("当前状态不可确认");
        }
        long max = as.getMaxAmount() == null ? 0L : as.getMaxAmount();
        long amount = refundAmount == null ? 0L : refundAmount;
        if (amount < 0 || amount > max) {
            return fail("退款金额需在 0 ~ " + max + " 分之间");
        }
        // 全额退款无需分销商同意；非全额退款必须说明原因
        boolean fullRefund = amount == max && max > 0;
        String remarkTrim = remark == null ? "" : remark.trim();
        if (!fullRefund && remarkTrim.isEmpty()) {
            return fail("非全额退款必须填写退款原因");
        }
        String refundFlow = null;
        if (fullRefund) {
            // 先执行资金回滚，失败则整体不落库，保持待仓库确认
            as.setRefundAmount(amount);
            try {
                refundFlow = refund(as, false, as.getSourceType() != null && as.getSourceType() == 2);
            } catch (Exception e) {
                return fail("退款执行失败：" + e.getMessage());
            }
        }
        ErpDistributionAfterSales update = new ErpDistributionAfterSales();
        update.setId(id);
        update.setRefundAmount(amount);
        update.setWarehouseTime(new Date());
        update.setWarehouseRemark(remarkTrim);
        if (fullRefund) {
            update.setStatus(ErpDistributionAfterSales.STATUS_DONE);
            update.setFinishTime(new Date());
            update.setDistributorTime(new Date());
            update.setDistributorRemark("全额退款，系统自动同意");
        } else {
            update.setStatus(ErpDistributionAfterSales.STATUS_WAIT_DISTRIBUTOR);
        }
        afterSalesMapper.updateById(update);
        writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                ErpDistributionOrderLog.ACTION_AS_WAREHOUSE_CONFIRM, null, null, as.getSellerId(), amount, null,
                ErpDistributionOrderLog.OPERATOR_SELLER, sellerId,
                "仓库确认退款金额（分）：" + amount + (fullRefund ? "，全额退款，系统自动同意" : "，原因：" + remarkTrim));
        if (fullRefund) {
            writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                    ErpDistributionOrderLog.ACTION_AS_DONE, null, ErpDistributionOrder.STATUS_REFUNDED, as.getOwnerId(),
                    amount, ErpDistributionOrderLog.SETTLE_REFUNDED, ErpDistributionOrderLog.OPERATOR_SYSTEM, null,
                    withFlow("全额退款完成，系统自动同意", refundFlow));
            return ok("全额退款已直接完成", id);
        }
        return ok("已确认，等待分销商确认", id);
    }

    @Override
    public Map distributorConfirm(Long id, Long ownerId, String remark) {
        ErpDistributionAfterSales as = afterSalesMapper.selectById(id);
        if (as == null) {
            return fail("售后单不存在");
        }
        if (!as.getOwnerId().equals(ownerId)) {
            return fail("只有分销商可以确认");
        }
        if (as.getStatus() == null || as.getStatus() != ErpDistributionAfterSales.STATUS_WAIT_DISTRIBUTOR) {
            return fail("当前状态不可确认");
        }
        ErpDistributionAfterSales update = new ErpDistributionAfterSales();
        update.setId(id);
        update.setStatus(ErpDistributionAfterSales.STATUS_EXECUTING);
        update.setDistributorTime(new Date());
        update.setDistributorRemark(remark);
        afterSalesMapper.updateById(update);
        // 已发货场景：只回滚资金，不动库存与商品
        String confirmFlow = refund(as, false, as.getSourceType() != null && as.getSourceType() == 2);
        ErpDistributionAfterSales done = new ErpDistributionAfterSales();
        done.setId(id);
        done.setStatus(ErpDistributionAfterSales.STATUS_DONE);
        done.setFinishTime(new Date());
        afterSalesMapper.updateById(done);
        writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                ErpDistributionOrderLog.ACTION_AS_DONE, null, ErpDistributionOrder.STATUS_REFUNDED, as.getOwnerId(),
                as.getRefundAmount(), ErpDistributionOrderLog.SETTLE_REFUNDED, ErpDistributionOrderLog.OPERATOR_BUYER, ownerId,
                withFlow("售后退款完成", confirmFlow));
        return ok("售后完成，已退款", id);
    }

    @Override
    public Map distributorReject(Long id, Long ownerId, String remark) {
        ErpDistributionAfterSales as = afterSalesMapper.selectById(id);
        if (as == null) {
            return fail("售后单不存在");
        }
        if (!as.getOwnerId().equals(ownerId)) {
            return fail("只有分销商可以操作");
        }
        if (as.getStatus() == null || as.getStatus() != ErpDistributionAfterSales.STATUS_WAIT_DISTRIBUTOR) {
            return fail("当前状态不可驳回");
        }
        ErpDistributionAfterSales update = new ErpDistributionAfterSales();
        update.setId(id);
        update.setStatus(ErpDistributionAfterSales.STATUS_WAIT_WAREHOUSE);
        update.setDistributorRemark(remark);
        update.setRejectCount((as.getRejectCount() == null ? 0 : as.getRejectCount()) + 1);
        if (remark != null && !remark.isEmpty()) {
            update.setReason(remark);
        }
        afterSalesMapper.updateById(update);
        writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                ErpDistributionOrderLog.ACTION_AS_DISTRIBUTOR_REJECT, null, null, as.getOwnerId(), null, null,
                ErpDistributionOrderLog.OPERATOR_BUYER, ownerId, "分销商不同意退款金额，已退回");
        return ok("已退回仓库方重新处理", id);
    }

    @Override
    public List<ErpDistributionAfterSales> listPendingBySeller(Long sellerId) {
        if (sellerId == null || sellerId <= 0) {
            return new java.util.ArrayList<>();
        }
        return afterSalesMapper.listPendingBySeller(sellerId);
    }

    @Override
    public int handleTimeout() {
        Date deadline = new Date(System.currentTimeMillis() - TIMEOUT_DAYS * 24L * 3600L * 1000L);
        List<ErpDistributionAfterSales> list = afterSalesMapper.listTimeout(deadline);
        int count = 0;
        for (ErpDistributionAfterSales as : list) {
            try {
                if (as.getStatus() == ErpDistributionAfterSales.STATUS_WAIT_WAREHOUSE) {
                    // 仓库超时未处理：按可退上限全额自动确认，并直接执行退款（等同全额退款免分销商同意）
                    long autoAmount = as.getMaxAmount() == null ? 0L : as.getMaxAmount();
                    as.setRefundAmount(autoAmount);
                    String timeoutFlow = refund(as, false, as.getSourceType() != null && as.getSourceType() == 2);
                    ErpDistributionAfterSales update = new ErpDistributionAfterSales();
                    update.setId(as.getId());
                    update.setRefundAmount(autoAmount);
                    update.setStatus(ErpDistributionAfterSales.STATUS_DONE);
                    update.setWarehouseTime(new Date());
                    update.setWarehouseRemark("超时未处理，系统按可退金额全额自动确认");
                    update.setDistributorTime(new Date());
                    update.setDistributorRemark("全额退款，系统自动同意");
                    update.setFinishTime(new Date());
                    update.setAutoFlag(1);
                    afterSalesMapper.updateById(update);
                    writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                            ErpDistributionOrderLog.ACTION_AS_TIMEOUT_CONFIRM, null, null, as.getSellerId(), autoAmount, null,
                            ErpDistributionOrderLog.OPERATOR_SYSTEM, null,
                            withFlow("仓库超时未处理，系统按可退金额全额自动确认", timeoutFlow));
                    writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                            ErpDistributionOrderLog.ACTION_AS_DONE, null, ErpDistributionOrder.STATUS_REFUNDED, as.getOwnerId(),
                            autoAmount, ErpDistributionOrderLog.SETTLE_REFUNDED, ErpDistributionOrderLog.OPERATOR_SYSTEM, null,
                            withFlow("超时自动退款完成", timeoutFlow));
                    count++;
                } else if (as.getStatus() == ErpDistributionAfterSales.STATUS_WAIT_DISTRIBUTOR) {
                    // 分销 7 天未处理：自动同意并执行
                    ErpDistributionAfterSales update = new ErpDistributionAfterSales();
                    update.setId(as.getId());
                    update.setStatus(ErpDistributionAfterSales.STATUS_EXECUTING);
                    update.setDistributorTime(new Date());
                    update.setDistributorRemark("超时未处理，系统自动同意");
                    update.setAutoFlag(1);
                    afterSalesMapper.updateById(update);
                    String distTimeoutFlow = refund(as, false, as.getSourceType() != null && as.getSourceType() == 2);
                    ErpDistributionAfterSales done = new ErpDistributionAfterSales();
                    done.setId(as.getId());
                    done.setStatus(ErpDistributionAfterSales.STATUS_DONE);
                    done.setFinishTime(new Date());
                    afterSalesMapper.updateById(done);
                    writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                            ErpDistributionOrderLog.ACTION_AS_DONE, null, ErpDistributionOrder.STATUS_REFUNDED, as.getOwnerId(),
                            as.getRefundAmount(), ErpDistributionOrderLog.SETTLE_REFUNDED,
                            ErpDistributionOrderLog.OPERATOR_SYSTEM, null,
                            withFlow("超时未处理，系统自动同意并退款", distTimeoutFlow));
                    count++;
                }
            } catch (Exception e) {
                log.error("售后超时处理失败, id={}", as.getId(), e);
            }
        }
        return count;
    }

    @Override
    public List<ErpDistributionAfterSales> listByDistOrderId(Long distOrderId) {
        return afterSalesMapper.listByDistOrderId(distOrderId);
    }

    @Override
    public ErpDistributionAfterSales getById(Long id) {
        return afterSalesMapper.selectById(id);
    }

    @Override
    public List<Long> listRefundedSellerIds(Long distOrderId) {
        if (distOrderId == null) {
            return new java.util.ArrayList<>();
        }
        List<Long> ids = afterSalesMapper.listRefundedSellerIds(distOrderId);
        return ids == null ? new java.util.ArrayList<>() : ids;
    }

    /**
     * 资金回滚：先全额退回仓库方手续费，再从仓库方扣退款金额给分销商
     * 订单未结算 -> 冻结；已结算 -> 余额；买家一律退到余额
     */
    private String refund(ErpDistributionAfterSales as, boolean rollbackStock, boolean reDispatchable) {
        long amount = as.getRefundAmount() == null ? 0L : as.getRefundAmount();
        ErpDistributionOrder dist = erpDistributionOrderService.getById(as.getDistOrderId());
        if (dist == null) {
            throw new RuntimeException("分销订单不存在");
        }
        boolean settled = dist.getStatus() != null && dist.getStatus() == ErpDistributionOrder.STATUS_FINISHED;
        BigDecimal refundAmt = new BigDecimal(amount);
        BigDecimal feeWarehouse = new BigDecimal(dist.getWarehouseFee() == null ? 0L : dist.getWarehouseFee());

        SysUser owner = userService.selectUserOne(dist.getOwnerId());
        SysUser seller = userService.selectUserOne(dist.getSellerId());
        SysUser admin = userService.selectUserOne(1L);
        if (owner == null || seller == null || admin == null) {
            throw new RuntimeException("账户信息不存在");
        }
        BigDecimal ownerBalBefore = owner.getBalance();
        BigDecimal ownerFzBefore = owner.getFreeze();
        BigDecimal sellerBalBefore = seller.getBalance();
        BigDecimal sellerFzBefore = seller.getFreeze();
        BigDecimal adminBalBefore = admin.getBalance();
        BigDecimal adminFzBefore = admin.getFreeze();
        boolean warehouseInitiated = as.getSourceType() != null && as.getSourceType() == 3;
        // 0. 先处理库存/单据；失败直接抛异常，资金一律不动（避免钱动了单没关）
        //    rollbackStock=true：解锁 + 平台各店库存 +1（书还能卖）
        //    仓库发起且 rollbackStock=false：库存清零报损 + 平台各店库存推 0
        if (rollbackStock) {
            rollbackStockForRefund(dist, as.getOutboundOrderId());
        } else if (warehouseInitiated) {
            writeOffStockForRefund(dist, as.getOutboundOrderId());
        }
        // 未完成（已下发）的单：钱在冻结里，退款动冻结；已完成的单：钱已转余额，退款动余额
        boolean useFreeze = !settled;
        // 1. 全额退回仓库方手续费（平台 -> 仓库）
        BigDecimal adminBefore = useFreeze ? admin.getFreeze() : admin.getBalance();
        changeMoney(admin, useFreeze, feeWarehouse.negate());
        writeSplitLog(dist, adminBefore, feeWarehouse.negate(), useFreeze ? admin.getFreeze() : admin.getBalance(),
                0L, admin, "售后退款-退回仓库方手续费（平台）", feeWarehouse, owner.getPhonenumber());
        BigDecimal sellerFeeBefore = useFreeze ? seller.getFreeze() : seller.getBalance();
        changeMoney(seller, useFreeze, feeWarehouse);
        writeSplitLog(dist, sellerFeeBefore, feeWarehouse, useFreeze ? seller.getFreeze() : seller.getBalance(),
                dist.getSellerId(), seller, "售后退款-退还仓库方手续费", "", owner.getPhonenumber());
        // 1.5 分销商手续费（platform_fee）：下发后 N 小时内申请售后才退回，超过则平台保留
        BigDecimal feePlatform = new BigDecimal(dist.getPlatformFee() == null ? 0L : dist.getPlatformFee());
        String feeNote = null;
        boolean platformFeeRefunded = false;
        boolean warehouseBearsFee = false;
        if (feePlatform.compareTo(BigDecimal.ZERO) > 0) {
            Date applyTime = as.getApplyTime() != null ? as.getApplyTime() : new Date();
            Date dispatchTime = logService.selectLastDispatchTime(dist.getId(), applyTime);
            long diffMs = dispatchTime == null ? Long.MAX_VALUE : applyTime.getTime() - dispatchTime.getTime();
            boolean withinWindow = diffMs >= 0 && diffMs <= feeRefundHours * 3600_000L;
            if (withinWindow && !feeAlreadyRefunded(dist)) {
                BigDecimal adminFeeBefore = useFreeze ? admin.getFreeze() : admin.getBalance();
                if (adminFeeBefore.compareTo(feePlatform) < 0) {
                    throw new RuntimeException("平台可用手续费不足，无法退回分销商手续费（可用 " + adminFeeBefore
                            + "，需退 " + feePlatform + "）");
                }
                changeMoney(admin, useFreeze, feePlatform.negate());
                writeSplitLog(dist, adminFeeBefore, feePlatform.negate(),
                        useFreeze ? admin.getFreeze() : admin.getBalance(),
                        0L, admin, "售后退款-退回分销商手续费（平台）", feePlatform, owner.getPhonenumber());
                BigDecimal ownerFeeBefore = owner.getBalance();
                changeMoney(owner, false, feePlatform);
                writeSplitLog(dist, ownerFeeBefore, feePlatform, owner.getBalance(),
                        dist.getOwnerId(), owner, "售后退款-退回分销商手续费", "", "");
                platformFeeRefunded = true;
                feeNote = "分销商手续费 " + yuanOfFen(feePlatform) + " 已退回（申请距下发 "
                        + durationText(diffMs) + "，在 " + feeRefundHours + " 小时内）";
            } else if (withinWindow) {
                feeNote = "分销商手续费 " + yuanOfFen(feePlatform) + " 本次未重复退回";
            } else if (warehouseInitiated) {
                // 仓库发起且超过 N 小时：手续费仍然全额退给分销商，但由仓库方承担（从仓库余额扣，扣完为止）
                BigDecimal ownerFeeBefore = owner.getBalance();
                changeMoney(owner, false, feePlatform);
                writeSplitLog(dist, ownerFeeBefore, feePlatform, owner.getBalance(),
                        dist.getOwnerId(), owner, "售后退款-退回分销商手续费", "", "");
                warehouseBearsFee = true;
                feeNote = "分销商手续费 " + yuanOfFen(feePlatform) + " 已退回，由仓库方承担（申请距下发 "
                        + (dispatchTime == null ? "未知" : durationText(diffMs)) + "，超过 " + feeRefundHours + " 小时）";
            } else {
                feeNote = "分销商手续费 " + yuanOfFen(feePlatform) + " 不退（申请距下发 "
                        + (dispatchTime == null ? "未知" : durationText(diffMs)) + "，超过 " + feeRefundHours + " 小时）";
            }
        }

        // 2. 从仓库方扣退款金额，退回分销商
        if (settled) {
            if (seller.getBalance().compareTo(refundAmt) < 0) {
                throw new RuntimeException("仓库方可用余额不足，无法退款");
            }
        } else if (seller.getFreeze().compareTo(refundAmt) < 0) {
            throw new RuntimeException("仓库方冻结余额不足，无法退款（冻结 " + seller.getFreeze() + "，需退 " + refundAmt + "）");
        }
        BigDecimal sellerBefore = useFreeze ? seller.getFreeze() : seller.getBalance();
        changeMoney(seller, useFreeze, refundAmt.negate());
        writeSplitLog(dist, sellerBefore, refundAmt.negate(), useFreeze ? seller.getFreeze() : seller.getBalance(),
                dist.getSellerId(), seller, "售后退款-仓库方扣回退款", "", owner.getPhonenumber());
        BigDecimal ownerBefore = owner.getBalance();
        changeMoney(owner, false, refundAmt);
        writeSplitLog(dist, ownerBefore, refundAmt, owner.getBalance(),
                dist.getOwnerId(), owner, "售后退款-退款到分销商", "", "");

        // 2.5 仓库方承担分销商手续费：退完款后从仓库余额扣（余额不足则扣到 0 为止）
        if (warehouseBearsFee) {
            BigDecimal sellerBal = seller.getBalance() == null ? BigDecimal.ZERO : seller.getBalance();
            BigDecimal deduct = sellerBal.compareTo(feePlatform) < 0 ? sellerBal : feePlatform;
            if (deduct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal before = seller.getBalance();
                changeMoney(seller, false, deduct.negate());
                writeSplitLog(dist, before, deduct.negate(), seller.getBalance(),
                        dist.getSellerId(), seller, "售后退款-仓库承担分销商手续费", "", "");
            }
            if (deduct.compareTo(feePlatform) < 0) {
                // 仓库余额不足：差额由平台余额垫付，保证账目平衡（后续可人工向仓库追偿）
                BigDecimal shortfall = feePlatform.subtract(deduct);
                BigDecimal adminBalBefore2 = admin.getBalance();
                changeMoney(admin, false, shortfall.negate());
                writeSplitLog(dist, adminBalBefore2, shortfall.negate(), admin.getBalance(),
                        0L, admin, "售后退款-仓库余额不足平台垫付", "", "");
                feeNote = (feeNote == null ? "" : feeNote + "；")
                        + "仓库余额不足，实扣 " + yuanOfFen(deduct) + "，差额 " + yuanOfFen(shortfall) + " 由平台垫付";
            }
        }

        // 3.5 未结算：未退款部分冻结转余额（售后结束，流水标记已结算，订单完成时不再重复解冻）
        if (!settled && amount > 0) {
            settleRemainingFrozen(dist, amount, platformFeeRefunded);
        }

        // 3.8 售后完成：该订单仍「待发货」的发货单一律取消，仓库不能再打单/发货
        try {
            Map cancelForm = new HashMap();
            cancelForm.put("user_id", String.valueOf(dist.getSellerId()));
            cancelForm.put("association_order_id", String.valueOf(dist.getId()));
            cancelForm.put("association_order_no", dist.getOrderNo() == null ? "" : dist.getOrderNo());
            cancelForm.put("remark", "售后退款完成，发货单自动取消");
            String cancelRes = com.order.main.util.InterfaceUtils.postForm(
                    com.order.main.util.UrlUtil.getNewWarehouse(), "/api/sales-order/cancel-pending-shipping", cancelForm);
            log.info("售后完成-取消待发货发货单 distId={} res={}", dist.getId(), cancelRes);
        } catch (Exception e) {
            log.error("售后完成-取消待发货发货单失败 distId={}", dist.getId(), e);
        }

        // 4. 平台退款 -> 已退款关闭；ERP 主动发起 -> 回到可重新下发
        if (reDispatchable) {
            erpDistributionOrderService.updateStatusById(dist.getId(), ErpDistributionOrder.STATUS_FAIL_UNSPLIT,
                    "售后退款完成，可重新下发");
        } else {
            erpDistributionOrderService.updateStatusById(dist.getId(), ErpDistributionOrder.STATUS_REFUNDED, "售后退款完成");
        }
        // 资金流动摘要（供操作日志「说明」列展示）
        StringBuilder flow = new StringBuilder();
        appendFlow(flow, "分销商余额 ", ownerBalBefore, owner.getBalance());
        appendFlow(flow, "仓库冻结 ", sellerFzBefore, seller.getFreeze());
        appendFlow(flow, "仓库余额 ", sellerBalBefore, seller.getBalance());
        appendFlow(flow, "平台冻结 ", adminFzBefore, admin.getFreeze());
        appendFlow(flow, "平台余额 ", adminBalBefore, admin.getBalance());
        if (feeNote != null) {
            flow.append(flow.length() == 0 ? "" : "；").append(feeNote);
        }
        return flow.length() == 0 ? null : flow.toString();
    }

    /**
     * 退款回滚库存：先关闭 PSI 销售单并解锁库存，再把平台各店库存 +1。
     * 失败直接抛异常，调用方在资金回滚之前，保证「钱不动、可重试」。
     */
    private void rollbackStockForRefund(ErpDistributionOrder dist, Long outboundOrderId) {
        Map unlock = new HashMap();
        unlock.put("user_id", String.valueOf(dist.getSellerId()));
        unlock.put("association_order_id", String.valueOf(dist.getId()));
        unlock.put("association_order_no", dist.getOrderNo());
        unlock.put("remark", "平台已退款，自动关闭");
        if (outboundOrderId != null && outboundOrderId > 0) {
            unlock.put("outbound_order_id", String.valueOf(outboundOrderId));
        }
        try {
            com.order.main.util.InterfaceUtils.postForm(com.order.main.util.UrlUtil.getNewWarehouse(),
                    "/api/sales-order/refund-close", unlock);
        } catch (Exception e) {
            String m = e.getMessage() == null ? "" : e.getMessage();
            if (m.contains("销售订单不存在")) {
                log.warn("退款回滚库存-销售单已关闭，跳过 refund-close distId={}", dist.getId());
            } else {
                throw new RuntimeException("关闭销售单失败：" + m);
            }
        }
        ErpGoodsOrder order = erpGoodsOrderMapper.selectById(dist.getSourceOrderId());
        if (order != null) {
            TShopGoodsPublishedService stockService = stockServiceProvider.getIfAvailable();
            if (stockService != null) {
                // 回滚库存：由各平台分支按自身上下架状态判断（闲鱼下架态直接上架，不叠加）
                stockService.synchronizeStockNew(String.valueOf(dist.getProductId()),
                        dist.getSellerId(), 1, 1, order);
            }
        }
    }

    /**
     * 仓库发起售后且选择「不回滚库存」：库存清零报损 + 平台各店库存推 0。
     * 失败抛异常，调用方保证资金不动。
     */
    private void writeOffStockForRefund(ErpDistributionOrder dist, Long outboundOrderId) {
        Map form = new HashMap();
        form.put("user_id", String.valueOf(dist.getSellerId()));
        form.put("association_order_id", String.valueOf(dist.getId()));
        form.put("association_order_no", dist.getOrderNo() == null ? "" : dist.getOrderNo());
        form.put("remark", "仓库缺货售后，库存清零报损");
        form.put("operator", "仓库(" + dist.getSellerId() + ")");
        form.put("operator_id", String.valueOf(dist.getSellerId()));
        if (outboundOrderId != null && outboundOrderId > 0) {
            form.put("outbound_order_id", String.valueOf(outboundOrderId));
        }
        try {
            com.order.main.util.InterfaceUtils.postForm(com.order.main.util.UrlUtil.getNewWarehouse(),
                    "/api/sales-order/refund-writeoff", form);
        } catch (Exception e) {
            String m = e.getMessage() == null ? "" : e.getMessage();
            if (m.contains("销售订单不存在")) {
                log.warn("仓库售后-销售单已关闭，跳过 refund-writeoff distId={}", dist.getId());
            } else {
                throw new RuntimeException("库存清零失败：" + m);
            }
        }
    }

    @Override
    public Map warehouseApply(String orderNo, Long sellerId, String reason, boolean rollbackStock, Long outboundOrderId) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return fail("分销单号不能为空");
        }
        if (reason == null || reason.trim().isEmpty()) {
            return fail("请填写缺货原因");
        }
        ErpDistributionOrder dist = erpDistributionOrderService.getByOrderNo(orderNo.trim());
        if (dist == null) {
            return fail("ERP分销订单不存在");
        }
        if (dist.getSellerId() == null || !dist.getSellerId().equals(sellerId)) {
            return fail("只有该订单的仓库方可以发起售后");
        }
        if (dist.getStatus() == null || (dist.getStatus() != ErpDistributionOrder.STATUS_PSI_OK
                && dist.getStatus() != ErpDistributionOrder.STATUS_FINISHED)) {
            return fail("该订单当前状态不支持发起售后");
        }
        if (afterSalesMapper.selectActiveByDistOrderId(dist.getId()) != null) {
            return fail("该订单已有进行中的售后申请");
        }
        Date now = new Date();
        ErpDistributionAfterSales as = new ErpDistributionAfterSales();
        as.setDistOrderId(dist.getId());
        as.setDistOrderNo(dist.getOrderNo());
        as.setSourceOrderId(dist.getSourceOrderId());
        as.setSourceOrderSn(dist.getSourceOrderSn());
        as.setOwnerId(dist.getOwnerId());
        as.setSellerId(dist.getSellerId());
        as.setProductId(dist.getProductId());
        as.setProductName(dist.getProductName());
        as.setReason(reason.trim());
        as.setRefundAmount(dist.getBookPrice());
        as.setMaxAmount(dist.getBookPrice());
        as.setStatus(ErpDistributionAfterSales.STATUS_DONE);
        as.setApplyTime(now);
        as.setWarehouseTime(now);
        as.setFinishTime(now);
        as.setWarehouseRemark("仓库发起售后（缺货）：" + reason.trim()
                + (rollbackStock ? "｜已回滚库存" : "｜库存清零报损"));
        as.setDistributorRemark("全额退款，系统自动同意");
        as.setRejectCount(0);
        as.setAutoFlag(0);
        as.setSourceType(3);
        as.setOutboundOrderId(outboundOrderId);
        afterSalesMapper.insert(as);
        writeLog(dist, ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                ErpDistributionOrderLog.ACTION_AS_WAREHOUSE_APPLY, dist.getStatus(), dist.getStatus(),
                dist.getSellerId(), dist.getBookPrice(), ErpDistributionOrderLog.SETTLE_FROZEN,
                ErpDistributionOrderLog.OPERATOR_SELLER, sellerId,
                "仓库发起售后（缺货）：" + reason.trim() + (rollbackStock ? "｜回滚库存" : "｜库存清零"));

        String flow;
        try {
            flow = refund(as, rollbackStock, true);
        } catch (Exception e) {
            ErpDistributionAfterSales closeAs = new ErpDistributionAfterSales();
            closeAs.setId(as.getId());
            closeAs.setStatus(ErpDistributionAfterSales.STATUS_CLOSED);
            closeAs.setFinishTime(new Date());
            closeAs.setDistributorRemark("仓库发起售后失败：" + e.getMessage());
            try {
                afterSalesMapper.updateById(closeAs);
            } catch (Exception ignore) {
            }
            return fail("售后处理失败：" + (e.getMessage() == null ? "系统异常" : e.getMessage()));
        }
        writeLog(dist, ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                ErpDistributionOrderLog.ACTION_AS_DONE, dist.getStatus(),
                ErpDistributionOrder.STATUS_FAIL_UNSPLIT, dist.getOwnerId(), dist.getBookPrice(),
                ErpDistributionOrderLog.SETTLE_REFUNDED, ErpDistributionOrderLog.OPERATOR_SELLER, sellerId,
                withFlow("仓库发起售后完成，全额退款，可重新下发", flow));
        return ok("已提交售后，全额退款完成，可重新下发", dist.getId());
    }

    /** 追加一段金额变动：余额 2925.07→2932.38(+7.31) */
    /**
     * 分账明细：只取「分销商」自己的资金流水（入账/出账/冻结），
     * 数据来自 PSI 主库 split_account_deduction_log 的真实流水。
     */
    @Override
    public Map listSplitLogs(Long distOrderId) {
        if (distOrderId == null) {
            return fail("参数错误");
        }
        ErpDistributionOrder dist = erpDistributionOrderService.getById(distOrderId);
        if (dist == null) {
            return fail("分销订单不存在");
        }
        String businessNo = buildBusinessNo(dist);
        String ownerId = dist.getOwnerId() == null ? "" : String.valueOf(dist.getOwnerId());
        List<Map> rows = new ArrayList<>();
        java.math.BigDecimal outTotal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal inTotal = java.math.BigDecimal.ZERO;
        String fetchError = null;
        try {
            int page = 1;
            while (page <= 20) {
                String url = "/api/split-account-deduction-log/list?business_no=" + businessNo
                        + "&page=" + page + "&page_size=100";
                String res = com.order.main.util.InterfaceUtils.getInterface(
                        com.order.main.util.UrlUtil.getNewWarehouse(), url);
                Map resMap = com.pdd.pop.sdk.common.util.JsonUtil.transferToObj(res, Map.class);
                Map dataMap = resMap == null ? null : (Map) resMap.get("data");
                if (dataMap == null) {
                    break;
                }
                List list = (List) dataMap.get("list");
                if (list == null || list.isEmpty()) {
                    break;
                }
                for (Object o : list) {
                    Map raw = (Map) o;
                    String createdBy = raw.get("created_by") == null ? "" : raw.get("created_by").toString();
                    // 只保留分销商自己的流水
                    if (!ownerId.equals(createdBy)) {
                        continue;
                    }
                    Map details = raw.get("deduction_details") instanceof Map
                            ? (Map) raw.get("deduction_details") : new HashMap();
                    String msg = details.get("msg") == null ? "" : details.get("msg").toString();
                    java.math.BigDecimal amt = decOf(raw.get("deduction_amount"));
                    java.math.BigDecimal before = decOf(raw.get("total_amount"));
                    java.math.BigDecimal after = decOf(raw.get("remaining_amount"));
                    boolean rolledBack = msg.contains("已冲正");
                    String type;
                    if (msg.contains("冻结")) {
                        type = amt.signum() < 0 ? "解冻" : "冻结";
                    } else {
                        type = amt.signum() < 0 ? "出账" : "入账";
                    }
                    String statusText;
                    if (rolledBack) {
                        statusText = "已冲正";
                    } else if (msg.contains("已售后结算")) {
                        statusText = "已售后结算";
                    } else if (msg.contains("已结算")) {
                        statusText = "已结算";
                    } else {
                        statusText = "正常";
                    }
                    Map<String, Object> row = new LinkedHashMap();
                    row.put("id", raw.get("id"));
                    row.put("time", fmtTime(raw.get("created_at")));
                    row.put("type", type);
                    row.put("amount", amt.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                    row.put("before", before.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                    row.put("after", after.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
                    row.put("status", statusText);
                    row.put("msg", cleanSplitMsg(msg));
                    row.put("rolledBack", rolledBack);
                    rows.add(row);
                    if (!rolledBack) {
                        if (amt.signum() < 0) {
                            outTotal = outTotal.add(amt.abs());
                        } else {
                            inTotal = inTotal.add(amt);
                        }
                    }
                }
                if (list.size() < 100) {
                    break;
                }
                page++;
            }
        } catch (Exception e) {
            log.error("查询分账明细失败 distId={}", distOrderId, e);
            fetchError = e.getMessage() == null ? "查询失败" : e.getMessage();
        }
        // PSI 按时间倒序返回，这里翻正序（老 → 新）
        java.util.Collections.reverse(rows);

        Map<String, Object> summary = new LinkedHashMap();
        summary.put("outTotal", outTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        summary.put("inTotal", inTotal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        try {
            SysUser owner = userService.selectUserOne(dist.getOwnerId());
            if (owner != null) {
                summary.put("balance", fenToYuan(owner.getBalance()));
                summary.put("freeze", fenToYuan(owner.getFreeze()));
            }
        } catch (Exception ignore) {
        }

        Map<String, Object> data = new LinkedHashMap();
        data.put("businessNo", businessNo);
        data.put("ownerId", dist.getOwnerId());
        data.put("orderNo", dist.getOrderNo());
        data.put("sourceOrderSn", dist.getSourceOrderSn());
        data.put("productName", dist.getProductName());
        data.put("list", rows);
        data.put("summary", summary);
        if (fetchError != null) {
            data.put("fetchError", fetchError);
        }
        Map m = new HashMap();
        m.put("code", 200);
        m.put("msg", "ok");
        m.put("data", data);
        return m;
    }

    /** 金额（元，字符串）→ BigDecimal；空值按 0 处理 */
    private static java.math.BigDecimal decOf(Object v) {
        if (v == null) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(v.toString());
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }

    /** 分 → 元（两位小数字符串） */
    private static String fenToYuan(java.math.BigDecimal fen) {
        if (fen == null) {
            return "0.00";
        }
        return fen.divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** 秒级时间戳 → yyyy-MM-dd HH:mm:ss */
    private static String fmtTime(Object ts) {
        if (ts == null) {
            return "-";
        }
        long sec;
        try {
            sec = new java.math.BigDecimal(ts.toString()).longValue();
        } catch (Exception e) {
            return "-";
        }
        if (sec <= 0) {
            return "-";
        }
        if (sec > 100000000000L) {
            sec = sec / 1000L;
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(sec * 1000L));
    }

    /** 去掉内部前后缀，只留可读说明 */
    private static String cleanSplitMsg(String msg) {
        if (msg == null || msg.isEmpty()) {
            return "-";
        }
        String m = msg;
        m = m.replace("-已售后结算", "").replace("-已结算", "")
                .replace("-已冲正（下发失败）", "").replace("-已售后退款", "");
        if (m.startsWith("售后退款-")) {
            m = m.substring("售后退款-".length());
        }
        if ("扣款金额".equals(m)) {
            return "下单扣款";
        }
        if ("余额增加".equals(m)) {
            return "入账（余额增加）";
        }
        if ("冻结资金增加".equals(m)) {
            return "冻结资金增加";
        }
        if ("冻结资金扣减".equals(m)) {
            return "解冻（冻结减少）";
        }
        if ("冻结资金扣除手续费".equals(m)) {
            return "扣除手续费";
        }
        return m;
    }

    /** 该单是否已经退过分销商手续费（幂等：异常重试时避免重复退） */
    private boolean feeAlreadyRefunded(ErpDistributionOrder dist) {
        try {
            String businessNo = buildBusinessNo(dist);
            String res = com.order.main.util.InterfaceUtils.getInterface(com.order.main.util.UrlUtil.getNewWarehouse(),
                    "/api/split-account-deduction-log/list?business_no=" + businessNo + "&page_size=200");
            Map resMap = com.pdd.pop.sdk.common.util.JsonUtil.transferToObj(res, Map.class);
            Map dataMap = resMap == null ? null : (Map) resMap.get("data");
            if (dataMap == null || dataMap.get("list") == null) {
                return false;
            }
            for (Object o : (List) dataMap.get("list")) {
                Map row = (Map) o;
                Map details = row.get("deduction_details") == null ? null : (Map) row.get("deduction_details");
                if (details != null && details.get("msg") != null
                        && details.get("msg").toString().contains("退回分销商手续费")) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("查询分销商手续费退款流水失败 distId={}", dist.getId(), e);
        }
        return false;
    }

    /** 与分账流水一致的 business_no：平台订单号-店铺ID-平台商品ID */
    private static String buildBusinessNo(ErpDistributionOrder dist) {
        String goodsId = "";
        if (dist.getItemList() != null) {
            Map item = com.pdd.pop.sdk.common.util.JsonUtil.transferToObj(dist.getItemList(), Map.class);
            if (item != null && item.get("goodsId") != null) {
                goodsId = item.get("goodsId").toString();
            }
        }
        return dist.getSourceOrderSn() + "-" + dist.getShopErpId() + "-" + goodsId;
    }

    /** 毫秒差转「X 小时 Y 分钟」 */
    private static String durationText(long diffMs) {
        if (diffMs < 0) {
            return "0 分钟";
        }
        long totalMin = diffMs / 60000L;
        long h = totalMin / 60;
        long m = totalMin % 60;
        return h > 0 ? h + " 小时 " + m + " 分钟" : m + " 分钟";
    }

    private static void appendFlow(StringBuilder sb, String label, BigDecimal before, BigDecimal after) {
        if (before == null || after == null) {
            return;
        }
        if (before.compareTo(after) == 0) {
            return;
        }
        BigDecimal diff = after.subtract(before);
        if (sb.length() > 0) {
            sb.append("；");
        }
        sb.append(label).append(yuanOfFen(before)).append("→").append(yuanOfFen(after))
                .append("(").append(diff.signum() > 0 ? "+" : "").append(yuanOfFen(diff)).append(")");
    }

    /** 分转元 */
    private static String yuanOfFen(BigDecimal fen) {
        if (fen == null) {
            return "0.00";
        }
        return fen.divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** 说明文本追加资金流 */
    private static String withFlow(String base, String flow) {
        if (flow == null || flow.isEmpty()) {
            return base;
        }
        String text = base + "｜资金：" + flow;
        return text.length() > 255 ? text.substring(0, 255) : text;
    }


    /** 未结算场景：未退款部分从冻结转入余额，并标记流水，避免订单完成时重复解冻 */
    /**
     * 未结算单售后结束：把该单剩余的冻结结清。
     * 仍处于冻结的只有两处：
     *   1) 仓库方：书价 - 本次退款额（售后结束，这部分归仓库）
     *   2) 平台：分销商手续费（超过 2 小时不退时，平台把它从冻结转成自己的余额）
     * 仓库手续费在退款第 1 步已全额退回仓库方、分销商手续费在 2 小时内也已退回，
     * 这两笔绝不能在这里再解冻一次，否则等于凭空多给平台一笔钱。
     */
    private void settleRemainingFrozen(ErpDistributionOrder dist, long refundAmount, boolean platformFeeRefunded) {
        try {
            // 1) 仓库方：书价 - 退款额，冻结转余额
            long bookPrice = dist.getBookPrice() == null ? 0L : dist.getBookPrice();
            long sellerRelease = bookPrice - refundAmount;
            if (sellerRelease < 0) {
                sellerRelease = 0;
            }
            if (sellerRelease > 0 && dist.getSellerId() != null) {
                SysUser seller = userService.selectUserOne(dist.getSellerId());
                if (seller != null) {
                    BigDecimal freeze = seller.getFreeze() == null ? BigDecimal.ZERO : seller.getFreeze();
                    if (freeze.compareTo(new BigDecimal(sellerRelease)) < 0) {
                        sellerRelease = freeze.longValue();
                    }
                    if (sellerRelease > 0) {
                        BigDecimal freezeBefore = seller.getFreeze();
                        seller.setFreeze(freezeBefore.subtract(new BigDecimal(sellerRelease)));
                        userService.updateMoney(seller);
                        writeSplitLog(dist, freezeBefore, new BigDecimal(-sellerRelease), seller.getFreeze(),
                                dist.getSellerId(), seller, "售后退款-未退部分解冻（冻结减少）", "", "");
                        BigDecimal balanceBefore = seller.getBalance();
                        seller.setBalance(balanceBefore.add(new BigDecimal(sellerRelease)));
                        userService.updateMoney(seller);
                        writeSplitLog(dist, balanceBefore, new BigDecimal(sellerRelease), seller.getBalance(),
                                dist.getSellerId(), seller, "售后退款-未退部分转余额（余额增加）", "", "");
                    }
                }
            }
            // 2) 平台：只有「分销商手续费没退」时，才把该笔手续费解冻成平台余额
            long platformFee = dist.getPlatformFee() == null ? 0L : dist.getPlatformFee();
            if (!platformFeeRefunded && platformFee > 0) {
                SysUser admin = userService.selectUserOne(1L);
                if (admin != null) {
                    BigDecimal freeze = admin.getFreeze() == null ? BigDecimal.ZERO : admin.getFreeze();
                    long release = Math.min(platformFee, freeze.longValue());
                    if (release > 0) {
                        BigDecimal freezeBefore = admin.getFreeze();
                        admin.setFreeze(freezeBefore.subtract(new BigDecimal(release)));
                        userService.updateMoney(admin);
                        writeSplitLog(dist, freezeBefore, new BigDecimal(-release), admin.getFreeze(),
                                0L, admin, "售后退款-未退部分解冻（冻结减少）", "", "");
                        BigDecimal balanceBefore = admin.getBalance();
                        admin.setBalance(balanceBefore.add(new BigDecimal(release)));
                        userService.updateMoney(admin);
                        writeSplitLog(dist, balanceBefore, new BigDecimal(release), admin.getBalance(),
                                0L, admin, "售后退款-未退部分转余额（余额增加）", "", "");
                    }
                }
            }
            // 3) 该单所有「冻结资金增加」流水统一标记已结算，订单完成时不再重复解冻
            markAllFrozenLogsSettled(dist);
        } catch (Exception e) {
            log.error("售后结算剩余冻结失败, distId={}", dist.getId(), e);
        }
    }

    /** 把该单所有「冻结资金增加」且未结算的流水标记为已结算 */
    private void markAllFrozenLogsSettled(ErpDistributionOrder dist) {
        try {
            String goodsId = "";
            if (dist.getItemList() != null) {
                Map item = com.pdd.pop.sdk.common.util.JsonUtil.transferToObj(dist.getItemList(), Map.class);
                if (item != null && item.get("goodsId") != null) {
                    goodsId = item.get("goodsId").toString();
                }
            }
            String businessNo = dist.getSourceOrderSn() + "-" + dist.getShopErpId() + "-" + goodsId;
            String res = com.order.main.util.InterfaceUtils.getInterface(com.order.main.util.UrlUtil.getNewWarehouse(),
                    "/api/split-account-deduction-log/list?business_no=" + businessNo + "&page_size=200");
            Map resMap = com.pdd.pop.sdk.common.util.JsonUtil.transferToObj(res, Map.class);
            Map dataMap = resMap == null ? null : (Map) resMap.get("data");
            if (dataMap == null || dataMap.get("list") == null) {
                return;
            }
            for (Object o : (List) dataMap.get("list")) {
                Map row = (Map) o;
                Object st = row.get("status");
                if (st != null && "1".equals(st.toString())) {
                    continue;
                }
                Object detailsObj = row.get("deduction_details");
                if (!(detailsObj instanceof Map)) {
                    continue;
                }
                Map details = (Map) detailsObj;
                if (details.get("msg") == null || !details.get("msg").toString().contains("冻结资金增加")) {
                    continue;
                }
                markLogSettled(row, details);
            }
        } catch (Exception e) {
            log.error("标记分账流水已结算失败, distId={}", dist.getId(), e);
        }
    }

    /** 写一条 PSI 分账流水（金额单位：分） */
    private void writeSplitLog(ErpDistributionOrder dist, BigDecimal totalAmount, BigDecimal deductionAmount,
                               BigDecimal remainingAmount, Long createdBy, SysUser user,
                               String msg, Object handlingFee, String fromPhone) {
        try {
            TShopGoodsPublishedService svc = stockServiceProvider.getIfAvailable();
            if (svc == null) {
                return;
            }
            String goodsId = "";
            if (dist.getItemList() != null) {
                Map item = com.pdd.pop.sdk.common.util.JsonUtil.transferToObj(dist.getItemList(), Map.class);
                if (item != null && item.get("goodsId") != null) {
                    goodsId = item.get("goodsId").toString();
                }
            }
            Map logMap = new HashMap();
            logMap.put("business_no", dist.getSourceOrderSn() + "-" + dist.getShopErpId() + "-" + goodsId);
            logMap.put("config_id", dist.getSplitConfigId());
            logMap.put("config_name", dist.getSplitConfigName());
            logMap.put("status", "0");
            net.minidev.json.JSONObject details = new net.minidev.json.JSONObject();
            details.put("erpOrderId", dist.getSourceOrderId());
            details.put("msg", msg);
            details.put("usePhone", user == null ? "" : user.getPhonenumber());
            details.put("userId", user == null ? null : user.getUserId());
            details.put("handlingFee", handlingFee == null ? "" : handlingFee);
            details.put("fromUser", fromPhone == null ? "" : fromPhone);
            svc.createSplitAccountDeductionLog(logMap, totalAmount, deductionAmount.toPlainString(), remainingAmount,
                    createdBy, details);
        } catch (Exception e) {
            log.error("写售后分账流水失败 distId={} msg={}", dist.getId(), msg, e);
        }
    }

    private void markLogSettled(Map row, Map details) {
        try {
            Map update = new HashMap();
            update.put("id", row.get("id").toString());
            details.put("msg", details.get("msg") + "-已售后结算");
            // PSI 更新接口要求 deduction_details 传 JSON 字符串，传 Map 会解析失败导致状态更新不生效
            update.put("deduction_details", com.pdd.pop.sdk.common.util.JsonUtil.transferToJson(details));
            update.put("status", "1");
            com.order.main.util.InterfaceUtils.postForm(com.order.main.util.UrlUtil.getNewWarehouse(),
                    "/api/split-account-deduction-log/update", update);
        } catch (Exception e) {
            log.error("标记分账流水已结算失败", e);
        }
    }
    private void changeMoney(SysUser user, boolean freeze, BigDecimal amount) {
        if (freeze) {
            user.setFreeze(user.getFreeze().add(amount));
        } else {
            user.setBalance(user.getBalance().add(amount));
        }
        userService.updateMoney(user);
    }

    @Override
    public Map cancel(Long id, Long ownerId, String remark) {
        ErpDistributionAfterSales as = afterSalesMapper.selectById(id);
        if (as == null) {
            return fail("售后单不存在");
        }
        if (!as.getOwnerId().equals(ownerId)) {
            return fail("只有分销商可以取消售后");
        }
        if (as.getStatus() == null || as.getStatus() != ErpDistributionAfterSales.STATUS_WAIT_WAREHOUSE) {
            return fail("只有等待仓库确认的售后可以取消");
        }
        ErpDistributionAfterSales update = new ErpDistributionAfterSales();
        update.setId(id);
        update.setStatus(ErpDistributionAfterSales.STATUS_CLOSED);
        update.setFinishTime(new java.util.Date());
        if (remark != null && !remark.isEmpty()) {
            update.setDistributorRemark(remark);
        }
        afterSalesMapper.updateById(update);
        writeLog(erpDistributionOrderService.getById(as.getDistOrderId()), ErpDistributionOrderLog.CATEGORY_AFTER_SALES,
                ErpDistributionOrderLog.ACTION_AS_CLOSE, null, null, as.getOwnerId(), null, null,
                ErpDistributionOrderLog.OPERATOR_BUYER, ownerId, "分销商取消售后");
        return ok("已取消售后", id);
    }

    private Map ok(String msg, Long id) {
        Map m = new HashMap();
        m.put("code", 200);
        m.put("msg", msg);
        m.put("data", id);
        return m;
    }

    private Map fail(String msg) {
        Map m = new HashMap();
        m.put("code", 400);
        m.put("msg", msg);
        return m;
    }

    /** 场景一：平台退款自动回滚（金额=书价全额） */
    @Override
    public Map autoRefund(Long distId, boolean rollbackStock) {
        ErpDistributionOrder dist = erpDistributionOrderService.getById(distId);
        if (dist == null) {
            return fail("分销订单不存在");
        }
        if (dist.getStatus() == null || dist.getStatus() == ErpDistributionOrder.STATUS_REFUNDED) {
            return ok("已退款，无需重复处理", distId);
        }
        // 平台退款优先：关闭进行中的 ERP 售后，且不再允许重新下发
        ErpDistributionAfterSales act = afterSalesMapper.selectActiveByDistOrderId(dist.getId());
        if (act != null) {
            ErpDistributionAfterSales close = new ErpDistributionAfterSales();
            close.setId(act.getId());
            close.setStatus(4);
            close.setDistributorRemark("平台已退款，售后关闭");
            close.setFinishTime(new java.util.Date());
            afterSalesMapper.updateById(close);
        }
        ErpDistributionAfterSales tmp = new ErpDistributionAfterSales();
        tmp.setDistOrderId(dist.getId());
        tmp.setRefundAmount(dist.getBookPrice());
        String platformFlow = refund(tmp, rollbackStock, false);
        writeLog(dist, ErpDistributionOrderLog.CATEGORY_AFTER_SALES, ErpDistributionOrderLog.ACTION_AS_AUTO_REFUND,
                dist.getStatus(), ErpDistributionOrder.STATUS_REFUNDED, dist.getOwnerId(), dist.getBookPrice(),
                ErpDistributionOrderLog.SETTLE_REFUNDED, ErpDistributionOrderLog.OPERATOR_SYSTEM, null,
                withFlow("平台退款，系统自动回滚" + (rollbackStock ? "（含库存回滚）" : ""), platformFlow));
        return ok("退款回滚完成", distId);
    }

    /**
     * 平台退款到达（出库已完成）时记录售后：status=2 执行中，锁定申请时间
     */
    @Override
    public void recordPlatformRefund(Long distOrderId) {
        try {
            ErpDistributionOrder dist = erpDistributionOrderService.getById(distOrderId);
            if (dist == null || afterSalesMapper.selectActiveByDistOrderId(distOrderId) != null) {
                return;
            }
            ErpDistributionAfterSales as = new ErpDistributionAfterSales();
            as.setDistOrderId(dist.getId());
            as.setDistOrderNo(dist.getOrderNo());
            as.setSourceOrderId(dist.getSourceOrderId());
            as.setSourceOrderSn(dist.getSourceOrderSn());
            as.setOwnerId(dist.getOwnerId());
            as.setSellerId(dist.getSellerId());
            as.setProductId(dist.getProductId());
            as.setProductName(dist.getProductName());
            as.setReason("平台退款，系统自动申请售后（待仓库处理）");
            as.setRefundAmount(dist.getBookPrice());
            as.setMaxAmount(dist.getBookPrice());
            as.setStatus(ErpDistributionAfterSales.STATUS_EXECUTING);
            as.setApplyTime(new java.util.Date());
            as.setRejectCount(0);
            as.setAutoFlag(1);
            as.setSourceType(1);
            afterSalesMapper.insert(as);
            log.info("平台退款-记录售后单 distOrderId={} asId={}", distOrderId, as.getId());
        } catch (Exception e) {
            log.error("平台退款-记录售后单失败 distOrderId={}", distOrderId, e);
        }
    }

    /**
     * 已出库已退款（status=7）：仓库处理后执行资金回滚，可选库存回滚
     */
    @Override
    public Map handlePendingRefund(Long distId, Long sellerId, boolean rollbackStock) {
        ErpDistributionOrder dist = erpDistributionOrderService.getById(distId);
        if (dist == null) {
            return fail("分销订单不存在");
        }
        if (sellerId != null && !sellerId.equals(dist.getSellerId())) {
            return fail("只有仓库方可以处理");
        }
        if (dist.getStatus() == null || dist.getStatus() != ErpDistributionOrder.STATUS_REFUND_PENDING) {
            return fail("订单不处于已退款待处理状态");
        }
        // 复用平台退款到达时记录的售后单（取当时的申请时间，用于 2 小时手续费判定）
        ErpDistributionAfterSales actAs = afterSalesMapper.selectActiveByDistOrderId(dist.getId());
        ErpDistributionAfterSales tmp = actAs != null ? actAs : new ErpDistributionAfterSales();
        if (actAs == null) {
            tmp.setDistOrderId(dist.getId());
            tmp.setRefundAmount(dist.getBookPrice());
        }
        String pendingFlow = refund(tmp, false, true);
        if (actAs != null) {
            ErpDistributionAfterSales done = new ErpDistributionAfterSales();
            done.setId(actAs.getId());
            done.setStatus(ErpDistributionAfterSales.STATUS_DONE);
            done.setFinishTime(new java.util.Date());
            afterSalesMapper.updateById(done);
        }
        if (rollbackStock) {
            ErpGoodsOrder order = erpGoodsOrderMapper.selectById(dist.getSourceOrderId());
            Map form = new HashMap();
            form.put("user_id", String.valueOf(dist.getSellerId()));
            form.put("association_order_id", String.valueOf(dist.getId()));
            form.put("association_order_no", dist.getOrderNo());
            com.order.main.util.InterfaceUtils.postForm(com.order.main.util.UrlUtil.getNewWarehouse(),
                    "/api/sales-order/refund-rollback-stock", form);
            if (order != null) {
                TShopGoodsPublishedService stockService = stockServiceProvider.getIfAvailable();
                if (stockService != null) {
                    stockService.synchronizeStockNew(String.valueOf(dist.getProductId()), dist.getSellerId(), 1, 1, order);
                }
            }
        }
        writeLog(dist, ErpDistributionOrderLog.CATEGORY_AFTER_SALES, ErpDistributionOrderLog.ACTION_AS_DONE,
                ErpDistributionOrder.STATUS_REFUND_PENDING, ErpDistributionOrder.STATUS_REFUNDED,
                dist.getSellerId(), dist.getBookPrice(), ErpDistributionOrderLog.SETTLE_REFUNDED,
                ErpDistributionOrderLog.OPERATOR_SELLER, sellerId,
                withFlow("仓库确认已处理退款" + (rollbackStock ? "（含库存回滚）" : ""), pendingFlow));
        return ok("退款处理完成", distId);
    }


    /** 查询 PSI 销售订单/出库状态，判断是否已出库（>=4 已出库/已发货） */
    private boolean isOutboundCompleted(ErpDistributionOrder dist) {
        try {
            String res = com.order.main.util.InterfaceUtils.getInterface(com.order.main.util.UrlUtil.getNewWarehouse(),
                    "/api/sales-order/query-by-erp-id?user_id=" + dist.getSellerId() + "&erp_id=" + dist.getId());
            Map resMap = com.pdd.pop.sdk.common.util.JsonUtil.transferToObj(res, Map.class);
            if (resMap == null || resMap.get("data") == null) {
                return false;
            }
            java.util.List list = (java.util.List) resMap.get("data");
            for (Object o : list) {
                Map item = (Map) o;
                // 出库单状态：1已创建 2拣货中 3已完成 4已取消 5发货中 6已发货
                // 只有「已完成/发货中/已发货」才算出库完成；已创建/拣货中/已取消都视为未完成
                Object obs = item.get("outbound_status");
                if (obs != null) {
                    try {
                        int os = Integer.parseInt(obs.toString());
                        if (os == 3 || os == 5 || os == 6) {
                            return true;
                        }
                    } catch (Exception ignore) {
                    }
                }
                // 销售单已发货（status=5）也视为出库完成
                Object ss = item.get("so_status");
                if (ss != null) {
                    try {
                        if (Integer.parseInt(ss.toString()) >= 5) {
                            return true;
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询PSI出库状态失败, distId={}", dist.getId(), e);
        }
        return false;
    }

    private void writeLog(ErpDistributionOrder dist, int category, String action,
                          Integer fromStatus, Integer toStatus, Long accountId, Long amount,
                          Integer settleStatus, int operatorType, Long operatorId, String remark) {
        if (dist == null) {
            return;
        }
        try {
            ErpDistributionOrderLog entry = new ErpDistributionOrderLog();
            entry.setDistOrderId(dist.getId());
            entry.setCategory(category);
            entry.setAction(action);
            entry.setFromStatus(fromStatus);
            entry.setToStatus(toStatus);
            entry.setSellerId(dist.getSellerId());
            entry.setAccountId(accountId);
            entry.setAmount(amount);
            entry.setSettleStatus(settleStatus);
            entry.setOperatorType(operatorType);
            entry.setOperatorId(operatorId);
            if (remark != null && remark.length() > 255) {
                remark = remark.substring(0, 255);
            }
            entry.setRemark(remark);
            logService.write(entry);
        } catch (Exception e) {
            log.error("写售后日志失败, distId={}", dist.getId(), e);
        }
    }
}
