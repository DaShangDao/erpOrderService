package com.order.main.entity;

import lombok.Data;

/**
 * 寄件人信息
 */
@Data
public class Sender {

    /**
     *  名称
     */
    private String name;
    /**
     *  电话
     */
    private String phone;
    /**
     *  座机
     */
    private String mobile;
    /**
     *  省
     */
    private String prov;
    /**
     *  市
     */
    private String city;
    /**
     *  区
     */
    private String county;
    /**
     *  详细地址
     */
    private String address;

}
