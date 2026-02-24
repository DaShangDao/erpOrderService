package com.order.main.dto.request;

import lombok.Data;

@Data
public class RefreshRequest {

    private String appId;

    private String grantType;

    private String appSecret;

    private String refreshToken;
}
