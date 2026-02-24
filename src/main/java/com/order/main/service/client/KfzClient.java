package com.order.main.service.client;

import com.dtflys.forest.annotation.Body;
import com.dtflys.forest.annotation.Post;
import com.dtflys.forest.annotation.Var;
import com.order.main.dto.request.RefreshRequest;
import com.order.main.dto.response.RefreshTokenResponse;
import org.springframework.stereotype.Service;

@Service
public interface KfzClient {

    @Post(value = "{myURL}/v1/oauth2/refresh", dataType = "json")
    RefreshTokenResponse refresh(@Var("myURL") String myURL, @Body RefreshRequest request);

}
