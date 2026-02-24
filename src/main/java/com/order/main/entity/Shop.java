package com.order.main.entity;

import lombok.Data;

import java.util.Date;

/**
 * 店铺主表对象 t_shop
 *
 * @author yxy
 * @date 2025-07-3
 */
@Data
public class Shop {

    /**
     *
     */
    private Long id;

    /**
     * 三方店铺id
     */
    private Long mallId;

    /**
     * 万里牛系统ID
     */
    private String shopNike;

    /**
     * 店铺类型  1 拼多多
     */
    private String shopType;

    /**
     * 分组
     */
    private String shopGroup;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺名称（对应平台的店铺名称）
     */
    private String shopAliasName;

    /**
     * 是否授权  0未授权 1已授权 2已过期
     */
    private String shopAuthorize;

    /**
     * 到期时间
     */
    private Date expirationTime;

    /**
     * 添加时间
     */
    private Date addTime;

    /**
     * 店铺key
     */
    private String shopKey;

    /**
     * token
     */
    private String token;

    /**
     * 刷新token 的token
     */
    private String refreshToken;


    /**
     * 店铺状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;

    /**
     * 租户编码
     */
    private String tenant_id;

    /**
     *  pdd 授权后的 可以同步订单的时间戳
     */
    private Long startUpdatedAt;

    /**
     * 第三方平台账号
     */
    private String account;

    /**
     * 第三方平台密码
     */
    private String password;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 是否开启同步订单：默认0-否 1-是
     */
    private Integer isSynOrder;

    /**
     * 创建人
     */
    private String createBy;

}
