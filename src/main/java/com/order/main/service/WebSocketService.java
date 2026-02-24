package com.order.main.service;


import com.order.main.util.WebSocketUtils;
import com.pdd.pop.sdk.message.WsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    private final WsClient wsClient;

    @Autowired
    public WebSocketService(WsClient wsClient) {
        this.wsClient = wsClient;
    }

    public void startConnection() {
        if (WebSocketUtils.getWsClientConnectionConfig()){
            wsClient.connect();
        }
    }
}