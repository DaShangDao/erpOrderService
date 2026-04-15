package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.entity.RunningTask;
import com.order.main.entity.Shop;
import com.order.main.entity.ShopGoodsPublished;
import com.order.main.service.IEditStockService;
import com.order.main.service.IRunningTaskByShopService;
import com.order.main.util.InterfaceUtils;
import com.order.main.util.Md5Util;
import com.order.main.util.PddUtil;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import com.pdd.pop.sdk.http.PopClient;
import com.pdd.pop.sdk.http.PopHttpClient;
import com.pdd.pop.sdk.http.api.pop.request.PddGoodsQuantityUpdateRequest;
import com.pdd.pop.sdk.http.api.pop.response.PddGoodsQuantityUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class EditStockServiceImpl implements IEditStockService {

    private final IRunningTaskByShopService runningTaskByShopService;

    /**
     * 修改闲鱼平台的商品库存
     * @param shop          店铺信息
     * @param itemId        商品id
     * @param number        库存数量
     */
    @Override
    public Map xyEditStock(Shop shop, String itemId, String number) {
        // 返回参数定义
        Map resultMap = new HashMap();
        try{
            // 记录库存 ，转为int类型
            int stock = Integer.parseInt(number);
            // 创建传参对象
            Map xyGoodsMap = new HashMap();
            // 闲管家商品id
            xyGoodsMap.put("product_id",Long.parseLong(itemId));
            // appId
            xyGoodsMap.put("appId",shop.getMallId());
            // appSecret
            xyGoodsMap.put("appSecret",shop.getToken());
            // user_name
            String[] userNames = new String[]{shop.getShopKey()};
            xyGoodsMap.put("user_name",userNames);
            // 库存
            xyGoodsMap.put("stock",stock);
            // 转为json字符串
            String mapJson = JsonUtil.transferToJson(xyGoodsMap);
            // 调用闲鱼DLL接口
            String resultPublish = DllInitializer.executeGoodsEditStock(mapJson);
            Map resultPublishMap = JsonUtil.transferToObj(resultPublish,Map.class);
            if (resultPublishMap.get("msg").equals("OK")){
                resultMap.put("code","200");
                resultMap.put("msg","更新库存成功");
            }else{
                resultMap.put("code","500");
                resultMap.put("msg",resultPublishMap.get("msg"));
            }

            try{
                // 库存为0 则执行下架操作
                if(stock <= 0){
                    //执行下架操作
                    String result = DllInitializer.executeGoodsDownShelf(mapJson);
                    Map resultDataMap = JsonUtil.transferToObj(result,Map.class);
                    if (resultDataMap.get("msg").equals("OK")){
                        resultMap.put("code","200");
                        resultMap.put("msg","库存为0下架成功");
                    }
                }else{
                    //库存不为0 则执行上架操作
                    DllInitializer.executeGoodsPublish(mapJson);
                }
            } catch (Exception e){
                resultMap.put("code","500");
                resultMap.put("msg","更新库存成功;额外操作异常");
            }


        }catch (Exception e){
            resultMap.put("code","500");
            resultMap.put("msg",e.getMessage());
        }

        return resultMap;
    }

    /**
     * 修改孔夫子平台的商品库存
     * @param token         店铺token
     * @param itemId        商品id
     * @param number        库存数量
     */
    @Override
    public Map kfzEditStock(String token, String itemId, String number) {

        Map resultData = new HashMap();

        Map map = new HashMap();
        // token
        map.put("token",token);
        // 商品id
        map.put("itemId",itemId);
        // 库存数量
        map.put("number",number);
        try{
            // 调用孔夫子接口
            String result = InterfaceUtils.getInterfacePost("http://146.56.227.42:8095", "/api/kfz/itemNumberUpdate", map);
            Map resultMap = JsonUtil.transferToObj(result,Map.class);

            String msg = "";
            if (resultMap.get("errorResponse") != null){
                Map errorResponse = (Map) resultMap.get("errorResponse");
                String errMsg = errorResponse.get("msg") == null ? "" : errorResponse.get("msg").toString();
                String errSubMsg = errorResponse.get("subMsg") == null ? "" : errorResponse.get("subMsg").toString();
                resultData.put("code","500");
                msg = errMsg + ";" + errSubMsg;
            }else if(resultMap.get("successResponse") != null){
                resultData.put("code","200");
                msg = "更新库存成功";
            }else{
                resultData.put("code","500");
                msg = "未知异常请联系管理员";
            }
            resultData.put("msg",msg);

        }catch (Exception e){
            resultData.put("code","500");
            resultData.put("msg",e.getMessage());
        }
        return resultData;
    }

    /**
     * 拼多多修改库存
     * @param shop      店铺信息
     * @param goodsId   商品id
     * @param quantity  库存数量
     */
    @Override
    public Map pddEditStock(Shop shop,String goodsId,String quantity) {
        // 返回参数
        Map resultMap = new HashMap();
        // 获取skuId
        String skuId = null;
        // 查询店铺商品信息
        RunningTask runningTask = runningTaskByShopService.selectByTrilateralId("t_running_task_"+shop.getId(),goodsId);
        // 店铺商品表存在改商品
        if(runningTask != null){
            // 解析商品数据
            Map successData = JsonUtil.transferToObj(runningTask.getSuccessData(),Map.class);
            // 获取skuId
            skuId = (String) successData.get("skuId");
        }
        // skuId不为空
        if(skuId != null){
            try {
                // 调用pdd接口
                PopClient client = new PopHttpClient(PddUtil.CLIENT_ID, PddUtil.CLIENT_SECRET);
                PddGoodsQuantityUpdateRequest request = new PddGoodsQuantityUpdateRequest();
                // 商品id
                request.setGoodsId(Long.parseLong(goodsId));
                // 库存数量
                request.setQuantity(Long.parseLong(quantity));
                // skuId
                request.setSkuId(Long.parseLong(skuId));
                // 调用修改库存接口
                PddGoodsQuantityUpdateResponse response = client.syncInvoke(request, shop.getToken());
                String msg = "";
                if(response.getErrorResponse() != null){
                    msg = response.getErrorResponse().getErrorMsg();
                    resultMap.put("code","500");
                }else if(!response.getGoodsQuantityUpdateResponse().getIsSuccess()){
                    resultMap.put("code","500");
                    msg = "更新库存失败";
                }else{
                    resultMap.put("code","200");
                    msg = "更新库存成功";
                }
                resultMap.put("msg",msg);
            } catch (Exception e) {
                resultMap.put("code","500");
                resultMap.put("msg","更新库存失败："+e.getMessage());
            }
        }else{
            resultMap.put("code","500");
            resultMap.put("msg","未获取到商品skuId,请重新执行店铺商品拉取任务");
        }
        return resultMap;
    }
}
