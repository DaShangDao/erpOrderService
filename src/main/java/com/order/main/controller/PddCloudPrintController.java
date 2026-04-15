package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.annotation.Get;
import com.order.main.dll.PddSimpleDllLoader;
import com.order.main.util.PddUtil;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拼多多云打印
 *
 * @author yxy
 * @date 2026-4-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pddCloudPrint")
public class PddCloudPrintController {


    /**
     * 拼多多云打印验证码
     * @param printerId 打印机id
     * @return
     */
    @GetMapping("/pddCloudPrintVerifyCode")
    public String pddCloudPrintVerifyCode(String printerId,String accessToken){
        JSONObject jsonObject = new JSONObject();
        JSONObject cloudPrintVerifyCodeRequest = new JSONObject();
        cloudPrintVerifyCodeRequest.put("printer_id",printerId);
        jsonObject.put("cloud_print_verify_code_request",cloudPrintVerifyCodeRequest);
        return PddSimpleDllLoader.executePddApi("PddCloudPrintVerifyCode", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,accessToken, jsonObject.toString());
    }

    /**
     * 云打印机绑定
     * @param printerId     打印机id
     * @param verifyCode    验证码
     * @param accessToken
     * @return
     */
    @GetMapping("/pddCloudPrinterBind")
    public String pddCloudPrinterBind(String printerId,String verifyCode,String accessToken){
        JSONObject jsonObject = new JSONObject();
        JSONObject cloudPrinterBindRequest = new JSONObject();
        cloudPrinterBindRequest.put("printer_id",printerId);
        cloudPrinterBindRequest.put("verify_code",verifyCode);
        jsonObject.put("cloud_printer_bind_request",cloudPrinterBindRequest);
        return PddSimpleDllLoader.executePddApi("PddCloudPrinterBind", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,accessToken, jsonObject.toString());
    }

    /**
     * 云打印
     * @return
     */
    @PostMapping(value = "/pddCloudPrint",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String pddCloudPrint(@RequestParam Map<String, String> data) {
        Map dataMap = JsonUtil.transferToObj(data.get("data").toString(),Map.class);
        // 商品数据
        List goodsDataList = (List) dataMap.get("dataList");
        Map goodsData = (Map) goodsDataList.get(0);
        // 自定义区域
        String zdyTxt = "";
        List printTemplateDataList = JsonUtil.transferToObj(dataMap.get("printTemplateData").toString(),List.class);
        for (Object object : printTemplateDataList){
            Map printTemplateData = (Map) object;
            Boolean checked = (Boolean) printTemplateData.get("checked");
            if (checked){
                String valueName = printTemplateData.get("value").toString();
                switch (valueName){
                    case "isGoodsName": zdyTxt += ",商品名称："+goodsData.get("goodsName"); break;
                    case "isGoodsArtno": zdyTxt += ",货号/原货号："+goodsData.get("artNo") + "/" + goodsData.get("originalArtNo"); break;
                    case "isGoodsIsbn": zdyTxt += ",ISBN："+goodsData.get("isbn"); break;
                    case "isGoodsNum": zdyTxt += ",数量："+goodsData.get("goodsCount"); break;
                }
            }
        }
        // 打印数据
        Map printDataMap = JsonUtil.transferToObj(dataMap.get("print_data").toString(),Map.class);
        // 打印机设置
        Map printer = (Map) dataMap.get("printer");
        if (printer.get("printType").equals("1")){
            Map res = new HashMap();
            res.put("code","500");
            res.put("msg","拼多多面单需要使用拼多多云打印机打印");
            return JsonUtil.transferToJson(res);
        }
        JSONObject jsonObject = new JSONObject();
        JSONObject cloudPrintRequest = new JSONObject();
        // 打印机id
        cloudPrintRequest.put("printer_id",printer.get("printer").toString());
        // 共享码
        cloudPrintRequest.put("share_code",printer.get("shareCode").toString());
        // 打印数据列表
        List<JSONObject> printDataList = new ArrayList<>();
        // 打印数据
        JSONObject printData = new JSONObject();
        // 自定区打印数据
        JSONObject customAreaPrintData = new JSONObject();
        // 自定义区打印数据
        JSONObject capData = new JSONObject();
        // 组装自定义区域数据
        String[] zdyTxtArr = zdyTxt.split(",");
        for (int i=0,j=1;i<zdyTxtArr.length;i++){
            if (!StringUtils.isEmpty(zdyTxtArr[i])){
                capData.put("key"+j,zdyTxtArr[i]);
                j++;
            }
        }
        customAreaPrintData.put("data",capData.toString());
        // 模板url
        customAreaPrintData.put("template_url","https://pos-file.pinduoduo.com/express-common-no-cache/common/xmltemplate/isvtemplate_722d04ae-3a1e-4007-bd6a-4ea3995796eb.xml");
        printData.put("custom_area_print_data",customAreaPrintData);
        // 面单打印数据
        JSONObject waybillPrinterData = new JSONObject();
        // 打印数据
        waybillPrinterData.put("data",printDataMap.get("encryptedData").toString());
        // 是否加密
        waybillPrinterData.put("encrypted",true);
        // 签名
        waybillPrinterData.put("signature",printDataMap.get("signature").toString());
        // 模板url
        waybillPrinterData.put("template_url",printDataMap.get("templateUrl").toString());
        // 版本
        waybillPrinterData.put("ver",printDataMap.get("ver").toString());
        printData.put("waybill_printer_data",waybillPrinterData);
        printDataList.add(printData);
        cloudPrintRequest.put("print_data_list",printDataList);
        jsonObject.put("cloud_print_request",cloudPrintRequest);
        String res = PddSimpleDllLoader.executePddApi("PddCloudPrint", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,dataMap.get("accessToken").toString(), jsonObject.toString());
        System.out.println(res);
        return "";
    }
}
