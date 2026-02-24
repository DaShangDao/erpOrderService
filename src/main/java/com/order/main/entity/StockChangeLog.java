package com.order.main.entity;

import lombok.Data;
import java.util.Date;

/**
 * 库存变更记录
 */
@Data
public class StockChangeLog {
    /**
     * 库存变更记录id
     */
    private Long id;
    /**
     * 商品id
     */
    private Long shopGoodsId;
    /**
     * 操作类型
     */
    private Integer type;
    /**
     * 关联id
     */
    private String aboutId;
    /**
     * 变更前库存
     */
    private Long beforeInv;
    /**
     * 变更后库存
     */
    private Long afterInv;
    /**
     * 创建人
     */
    private Long createBy;
    /**
     * 用户名
     */
    private Long updateBy;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;
}
