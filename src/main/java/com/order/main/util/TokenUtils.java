package com.order.main.util;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dto.request.RefreshRequest;
import com.order.main.dto.request.UpdateTokenRequest;
import com.order.main.dto.response.RefreshTokenResponse;
import com.order.main.service.client.ErpClient;
import com.order.main.service.client.KfzClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class TokenUtils {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    @Autowired
    private KfzClient kfzClient;

    @Autowired
    private ErpClient erpClient;

    // 刷新Token
    public  String refreshToken(String refreshToken, Long shopId) {
        try {
            RefreshRequest refreshRequest = new RefreshRequest();
            refreshRequest.setAppId(ClientConstantUtils.KFZ_APP_ID+"");
            refreshRequest.setGrantType("refresh_token");
            refreshRequest.setAppSecret(ClientConstantUtils.KFZ_APP_SECRET);
            refreshRequest.setRefreshToken(refreshToken);
            RefreshTokenResponse refreshTokenVo = kfzClient.refresh(ClientConstantUtils.KFZ_URL, refreshRequest);
            log.info("刷新令牌响应：{}", JSONObject.toJSONString(refreshTokenVo));
            if (refreshTokenVo.getErrCode().equals(0)) {
                String accessToken = refreshTokenVo.getResult().getAccessToken();
                String newRefreshToken = refreshTokenVo.getResult().getRefreshToken();
                Long expiresAt = refreshTokenVo.getResult().getExpiresAt();
                UpdateTokenRequest updateTokenRequest = new UpdateTokenRequest();
                updateTokenRequest.setShopId(shopId);
                updateTokenRequest.setShopType("2");
                updateTokenRequest.setAccessToken(accessToken);
                updateTokenRequest.setRefreshToken(newRefreshToken);
                updateTokenRequest.setExpirationTime(Date.from(Instant.ofEpochSecond(expiresAt)));
                Boolean updateTokenResult = erpClient.updateToken(ClientConstantUtils.ERP_URL, updateTokenRequest);
                if (!updateTokenResult) throw new Exception("更新token到数据库失败，请重新授权：门店Id-" + shopId);
                return accessToken;
            }
            log.error("调用孔夫子刷新token异常-{}", JSONObject.toJSONString(refreshTokenVo));
            throw new Exception("调用孔夫子刷新token异常");
        } catch (Exception e) {
            throw new RuntimeException("调用孔夫子刷新token异常" + e);
        }
    }

}
