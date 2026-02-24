package com.order.main.service.client;

import com.dtflys.forest.annotation.Get;
import com.dtflys.forest.annotation.LogEnabled;
import com.dtflys.forest.annotation.Query;
import com.dtflys.forest.annotation.Var;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ShopGoodsPublishedClient {



    /**
     * 商品发布驳回-类目修改cat_id
     */
    @Get(value = "{baseUrl}/zhishu/shopGoodsPublished/uodateCatId", dataType = "json")
    @LogEnabled(value = false)  // 关闭该方法的日志
    String uodateCatId(@Var("baseUrl") String baseUrl, @Query("shopType") String shopType, @Query("mallId") Long mallId, @Query("platformId") String platformId);


    /**
     * 删除发布失败商品
     */
    @Get(value = "{baseUrl}/zhishu/shopGoodsPublished/delShopGoodsPublished", dataType = "json")
    @LogEnabled(value = false)  // 关闭该方法的日志
    Boolean delShopGoodsPublished(@Var("baseUrl") String baseUrl,@Query("shopType") String shopType,@Query("mallId") Long mallId,@Query("platformId") String platformId,@Query("rejectComment") String rejectComment);


}
