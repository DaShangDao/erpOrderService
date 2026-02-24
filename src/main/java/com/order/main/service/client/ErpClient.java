package com.order.main.service.client;

import com.dtflys.forest.annotation.*;
import com.order.main.dto.request.UpdateTokenRequest;
import org.springframework.stereotype.Service;


@Service
public interface ErpClient {

    /**
     * 更新token
     *
     * @param myURL
     * @param request
     * @return
     */
    @Put(value = "{myURL}/zhishu/shop/updateToken", dataType = "json", headers = {"Content-Type: application/json"})
    Boolean updateToken(@Var("myURL") String myURL, @Body UpdateTokenRequest request);
}
