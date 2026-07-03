package com.order.main.entity;

import lombok.Data;

/**
 * psi用户信息
 */
@Data
public class PsiEmployees {

    /**
     * 关于ID
     */
    private Long aboutId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 姓名
     */
    private String name;

    /**
     * 电话
     */
    private String phone;

    /**
     * 来源
     */
    private String from;

    /**
     * 最后登录时间
     */
    private Long lastLoginAt;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 编码
     */
    private String code;

    /**
     * 分账配置ID
     */
    private Long splitAccountConfigId;

    // ========== 以下字段来自 split_account_config 表（left join） ==========

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则值
     */
    private String ruleValue;

    /**
     * 状态
     */
    private Integer status;


}