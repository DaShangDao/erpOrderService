package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.PddSimpleDllLoader;
import com.order.main.entity.SinglePrint;
import com.order.main.mapper.SinglePrintMapper;
import com.order.main.service.ISinglePrintService;
import com.order.main.util.PddUtil;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SinglePrintServiceImpl implements ISinglePrintService {

    private final SinglePrintMapper baseMapper;

    @Override
    public SinglePrint getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public SinglePrint getByMailNo(String mailNo) {
        return baseMapper.selectByMailNo(mailNo);
    }

    @Override
    public List<SinglePrint> getPageList(SinglePrint singlePrint) {
        singlePrint.setPageNum((singlePrint.getPageNum() - 1) * singlePrint.getPageSize());
        return baseMapper.selectPageList(singlePrint);
    }

    @Override
    public int count(SinglePrint singlePrint) {
        return baseMapper.count(singlePrint);
    }

    @Override
    public List<SinglePrint> getList(SinglePrint query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SinglePrint singlePrint) {
        return baseMapper.insert(singlePrint) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SinglePrint singlePrint) {
        return baseMapper.update(singlePrint) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMailNo(String mailNo) {
        return baseMapper.deleteByMailNo(mailNo) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Long> ids) {
        return baseMapper.deleteBatch(ids) > 0;
    }

    /**
     * 打印
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map printView(Map fastMailVo, String mailNo, String orderNo, List itemList){

        // 快递账号类型  1 网点  2 拼多多
        String fastMailType = fastMailVo.get("fastMailType").toString();

        // 返回值对象定义
        Map result = new HashMap();

        if (fastMailType.equals("1")){
            // 网点打印
            Map params = new HashMap();
            //也是app-key
            params.put("appid","004064");
            params.put("partner_id",fastMailVo.get("partnerId").toString());
            params.put("secret",fastMailVo.get("secret").toString());
            List<Map> orders = new ArrayList();
            Map order = new HashMap();
            /**
             * 运单号
             */
            order.put("mailno", mailNo);
            orders.add(order);
            params.put("orders",orders);
            String jsonData = JsonUtil.transferToJson(params);
            String res = DllInitializer.ydBmGetPdfInfo(jsonData, "004064", "eed7ae222b8541deae79cdfc318b7aa8");
            Map resMap = JsonUtil.transferToObj(res,Map.class);
            if (resMap.get("code").equals("0000") && resMap.get("message").equals("请求成功")){
                List dataList = (List) resMap.get("data");
                Map data = (Map) dataList.get(0);
                result.put("code","200");
                result.put("msg","获取快递订单成功");
                result.put("pdfInfo",data.get("pdfInfo").toString());
                result.put("mailNo",mailNo);
                result.put("fastMailType",fastMailType);
                List list = new ArrayList();
                for (Object object : itemList){
                    Map item = (Map) object;
                    Map goodsMap = new HashMap();
                    goodsMap.put("goodsName",item.get("itemName").toString());
                    goodsMap.put("goodsCount",item.get("itemNum").toString());
                    goodsMap.put("isbn",item.get("isbn") == null ? "" : item.get("isbn").toString());
                    goodsMap.put("artNo",item.get("artNo") == null ? "" : item.get("artNo").toString());
                    goodsMap.put("originalArtNo",item.get("originalArtNo") == null ? "" : item.get("originalArtNo").toString());
                    list.add(goodsMap);
                }
                result.put("dataList",list);
                return result;
            }
            result.put("code","500");
            result.put("msg",resMap.get("message").toString());
            return result;
        }else if(fastMailType.equals("2")) {
            try {
                List<JSONObject> paramList = new ArrayList<>();
                JSONObject param = new JSONObject();
                // 请求id
                param.put("object_id", orderNo);
                // 快递单号
                param.put("waybill_code", mailNo);
                // 快递公司编码
                param.put("wp_code", fastMailVo.get("type").toString());
                paramList.add(param);
                Map remarkData = JsonUtil.transferToObj(fastMailVo.get("remark").toString(), Map.class);
                // 拼多多打印
                String res = PddSimpleDllLoader.executePddApi("PddWaybillQueryByWaybillcode", PddUtil.CLIENT_ID, PddUtil.CLIENT_SECRET, remarkData.get("token").toString(), JsonUtil.transferToJson(paramList));
                Map resMap = JsonUtil.transferToObj(res, Map.class);
                Map pddWaybillQueryByWaybillcodeResponse = (Map) resMap.get("pdd_waybill_query_by_waybillcode_response");
                List modules = Collections.singletonList(pddWaybillQueryByWaybillcodeResponse.get("modules"));
                List module = (List) modules.get(0);
                Map waybillCloudPrintResponse = (Map) ((Map) module.get(0)).get("waybill_cloud_print_response");

                List list = new ArrayList();
                for (Object object : itemList){
                    Map item = (Map) object;
                    Map goodsMap = new HashMap();
                    goodsMap.put("goodsName",item.get("itemName").toString());
                    goodsMap.put("goodsCount",item.get("itemNum").toString());
                    goodsMap.put("isbn",item.get("isbn") == null ? "" : item.get("isbn").toString());
                    goodsMap.put("artNo",item.get("artNo") == null ? "" : item.get("artNo").toString());
                    goodsMap.put("originalArtNo",item.get("originalArtNo") == null ? "" : item.get("originalArtNo").toString());
                    list.add(goodsMap);
                }
                waybillCloudPrintResponse.put("dataList", list);
                waybillCloudPrintResponse.put("accessToken",remarkData.get("token").toString());
                result.put("code", "200");
                result.put("msg", "获取快递订单成功");
                result.put("data", waybillCloudPrintResponse);
                result.put("mailNo", mailNo);
                result.put("fastMailType", fastMailType);
                return result;
            } catch (Exception e) {
                result.put("code", "500");
                result.put("msg", "获取订单异常");
                return result;
            }
        }else {
            result.put("code", "500");
            result.put("msg", "快递账号类型错误");
            return result;
        }
    }
}