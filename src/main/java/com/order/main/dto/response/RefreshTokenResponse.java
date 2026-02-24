package com.order.main.dto.response;

import lombok.Data;

@Data
public class RefreshTokenResponse {

    // 错误码
    private Integer errCode;

    // 错误信息
    private String errMessage;

    // 结果
    private Result result;

    // 状态
    private Boolean status;

    @Data
    public static class Result{

        // access_token
        private String accessToken;

        // refresh_token
        private String refreshToken;

        // 用户ID
        private Long userId;

        // 过期时间
        private Long expiresAt;

        // refresh_token 过期时间
        private Long refreshExpiresAt;

        // 授权类型
        private String grantType;

    }
}
