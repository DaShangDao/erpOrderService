package com.order.main.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分润配置信息
 */
@Data
public class PsiSplitAccountConfig {

    // 分润方
    private String productType;
    // 分润百分比
    private BigDecimal ratio;
    // 分润加价
    private BigDecimal addAmount;
}
