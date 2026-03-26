package com.order.main.util;

import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import com.pdd.pop.sdk.http.PopClient;
import com.pdd.pop.sdk.http.PopHttpClient;
import com.pdd.pop.sdk.http.api.pop.request.PddCloudprintStdtemplatesGetRequest;
import com.pdd.pop.sdk.http.api.pop.request.PddOpenDecryptBatchRequest;
import com.pdd.pop.sdk.http.api.pop.response.PddCloudprintStdtemplatesGetResponse;
import com.pdd.pop.sdk.http.api.pop.response.PddOpenDecryptBatchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public final class PddUtil {

    public static String CLIENT_ID = "203c5a7ba8bd4b8488d5e26f93052642";

    public static String CLIENT_SECRET = "892ffaa86e12b7a3d8d2942b669d9aa520ad8179";


    /**
     * 获取解密对象
     * @param dataTag
     * @param encryptedData
     * @return
     */
    public static PddOpenDecryptBatchRequest.DataListItem getDataListItem(String dataTag,String encryptedData){
        if(StringUtils.isEmpty(encryptedData)){
            return null;
        }
        PddOpenDecryptBatchRequest.DataListItem dataListItem = new PddOpenDecryptBatchRequest.DataListItem();
        dataListItem.setDataTag(dataTag);
        dataListItem.setEncryptedData(encryptedData);
        return dataListItem;
    }

    /**
     * pdd解密接口
     * @param accessToken
     */
    public static List decryptBatch(String accessToken,List<PddOpenDecryptBatchRequest.DataListItem> dataList){
        PddOpenDecryptBatchRequest request = new PddOpenDecryptBatchRequest();
        request.setDataList(dataList);

        String requestStr = JsonUtil.transferToJson(request);
        Map map = new HashMap();
        map.put("accessToken", accessToken);
        map.put("requestStr",requestStr);
        String responseStr = InterfaceUtils.postForm("http://pdd.buzhiyushu.cn","/api/pdd/auth/decryptBatch",map);

        Map responseMap = JsonUtil.transferToObj(responseStr,Map.class);
        if (responseMap != null && null == responseMap.get("errorResponse")) {
            Map openDecryptBatchResponseMap = (Map) responseMap.get("openDecryptBatchResponse");

            return (List) openDecryptBatchResponseMap.get("dataDecryptList");
        }else{
            return new ArrayList<>();
        }
    }

    /**
     * 获取拼多多标准打印模板
     * @param wpCode
     * @return
     */
    public static String getCloudprintStdtemplates(String wpCode){
        PopClient client = new PopHttpClient(CLIENT_ID, CLIENT_SECRET);
        PddCloudprintStdtemplatesGetRequest request = new PddCloudprintStdtemplatesGetRequest();
        request.setWpCode(wpCode);
        PddCloudprintStdtemplatesGetResponse response = null;
        try {
             response = client.syncInvoke(request);

           PddCloudprintStdtemplatesGetResponse.InnerPddCloudprintStdtemplatesGetResponse innerPddCloudprintStdtemplatesGetResponse =  response.getPddCloudprintStdtemplatesGetResponse();
           PddCloudprintStdtemplatesGetResponse.InnerPddCloudprintStdtemplatesGetResponseResult result = innerPddCloudprintStdtemplatesGetResponse.getResult();
            List<PddCloudprintStdtemplatesGetResponse.InnerPddCloudprintStdtemplatesGetResponseResultDatasItem> datas = result.getDatas();
            for (PddCloudprintStdtemplatesGetResponse.InnerPddCloudprintStdtemplatesGetResponseResultDatasItem datasItem : datas){
                if (datasItem.getWpCode().equals(wpCode)){
                    List<PddCloudprintStdtemplatesGetResponse.InnerPddCloudprintStdtemplatesGetResponseResultDatasItemStandardTemplatesItem> standardTemplates = datasItem.getStandardTemplates();
                    for (PddCloudprintStdtemplatesGetResponse.InnerPddCloudprintStdtemplatesGetResponseResultDatasItemStandardTemplatesItem standardTemplate : standardTemplates){
                        if (standardTemplate.getStandardTemplateName().equals("快递一联单")){
                            return standardTemplate.getStandardTemplateUrl();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "";
    }
}
