package com.order.main.dto.request;

import lombok.Data;

import java.util.Date;

@Data
public class UpdateTokenRequest {

    private Long shopId;

    private String shopType;

    private String accessToken;

    private String refreshToken;

    private Date expirationTime;

}
