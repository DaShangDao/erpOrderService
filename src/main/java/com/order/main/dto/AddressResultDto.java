package com.order.main.dto;

import lombok.Data;

@Data
public class AddressResultDto {

    private String province;      // 省/直辖市/自治区/特别行政区
    private String city;          // 市
    private String district;      // 区/县
    private String street;        // 街道/乡镇
    private String detail;        // 详细地址
    private String fullAddress;   // 完整地址

}
