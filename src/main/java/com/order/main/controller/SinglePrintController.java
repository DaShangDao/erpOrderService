package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.PddSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.CourierLog;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.SinglePrint;
import com.order.main.service.ISinglePrintService;
import com.order.main.util.PddUtil;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/singlePrint")
public class SinglePrintController {

    private final ISinglePrintService singlePrintService;

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public SinglePrint getById(@PathVariable Long id) {
        return singlePrintService.getById(id);
    }

    /**
     * 根据快递单号查询
     */
    @GetMapping("/getByMailNo/{mailNo}")
    public SinglePrint getByMailNo(@PathVariable String mailNo) {
        return singlePrintService.getByMailNo(mailNo);
    }

    /**
     * 分页查询列表
     */
    @GetMapping("/getList")
    public Map<String, Object> getList(SinglePrint singlePrint) {
        Map<String, Object> map = new HashMap<>();
        map.put("data", singlePrintService.getPageList(singlePrint));
        map.put("total", singlePrintService.count(singlePrint));
        return map;
    }

    /**
     * 新增
     */
    @PostMapping("/addSinglePrint")
    public Map addSinglePrint(@RequestParam Map map) {
        // 返回值对象定义
        Map result = new HashMap();
        String orderSn = map.get("orderSn").toString();
        Map fastMailVo = JsonUtil.transferToObj(map.get("fastMailVo").toString(),Map.class);
        // 快递账号类型  1 网点  2 拼多多
        String fastMailType = fastMailVo.get("fastMailType").toString();
        // 快递公司编码
        String wpCode = fastMailVo.get("type").toString();
        // 运费模板
        Map logisticsVo = JsonUtil.transferToObj(map.get("logisticsVo").toString(),Map.class);
        // 快递单号
        String waybillCode = "";


        boolean isOutletType = fastMailType.equals("1");
        // 发货地址信息
        Map remarkData = isOutletType ? null : JsonUtil.transferToObj(fastMailVo.get("remark").toString(),Map.class);
        // 发货人 - 省
        String senderProvince = isOutletType ? logisticsVo.get("deliveryProvince").toString() : remarkData.get("province").toString();
        // 发货人 - 市
        String senderCity = isOutletType ? logisticsVo.get("deliveryCity").toString() : remarkData.get("city").toString();
        // 发货人 - 区
        String senderDistrict = isOutletType ? logisticsVo.get("deliveryArea").toString() : remarkData.get("district").toString();
        // 发货人 - 详细地址
        String senderDetail = isOutletType ? logisticsVo.get("fullAddress").toString() : remarkData.get("detail").toString();

        // 定义对象
        SinglePrint singlePrint = new SinglePrint();
        // 发货人
        singlePrint.setSenderName(logisticsVo.get("contact").toString());
        // 发货人 - 联系电话
        singlePrint.setSenderPhone(logisticsVo.get("phoneNumber").toString());
        // 发货人 - 地址
        singlePrint.setSenderAddress(senderProvince+","+senderCity+","+senderDistrict+","+senderDetail);
        // 收件人
        singlePrint.setReceiverName(map.get("name").toString());
        // 收件人 - 联系电话
        singlePrint.setReceiverPhone(map.get("phone").toString());
        // 收件人 - 地址
        singlePrint.setReceiverAddress(map.get("province").toString()+","+map.get("city").toString()+","+map.get("district").toString()+","+map.get("detail").toString());
        // 物品名称
        singlePrint.setItemName(map.get("itemName").toString());
        // 数量
        singlePrint.setItemNum(Integer.parseInt(map.get("itemNum").toString()));
        // 物品备注
        singlePrint.setItemRemark(map.get("itemRemark") == null ? "" : map.get("itemRemark").toString());
        // 打单账号id
        singlePrint.setFastMailId(Long.parseLong(fastMailVo.get("id").toString()));
        // 打单账号信息
        singlePrint.setFastMailText(map.get("fastMailVo").toString());
        // 创建人
        singlePrint.setCreateBy(Long.parseLong(fastMailVo.get("createBy").toString()));
        // 状态
        singlePrint.setStatus("1");


        if (fastMailType.equals("1")){
            if (fastMailVo.get("type").equals("YUNDA")){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("appid", "004064");
                jsonObject.put("partner_id",fastMailVo.get("partnerId").toString());
                jsonObject.put("secret",fastMailVo.get("secret").toString());
                List<Object> orders = new ArrayList<>();
                JSONObject order = new JSONObject();
                // 订单唯一序列号 由字母、数字、下划线组成，必须保证唯一，请对特殊符号进行过滤
                order.put("order_serial_no",orderSn);
                // 大客户系统订单的订单号可与订单唯一序列号相同
                order.put("khddh",orderSn);
                // 发件人对象
                JSONObject sender = new JSONObject();
                // 发件人姓名
                sender.put("name",singlePrint.getSenderName());
                // 详细地址 必须添加省市区并以半角逗号隔开
                sender.put("address",singlePrint.getSenderAddress());
                // 手机号
                sender.put("mobile",singlePrint.getSenderPhone());
                order.put("sender",sender);
                // 收件人对象
                JSONObject receiver = new JSONObject();
                // 收件人姓名
                receiver.put("name",singlePrint.getReceiverName());
                // 详细地址 必须添加省市区并以半角逗号隔开
                receiver.put("address",singlePrint.getReceiverAddress());
                // 手机号
                receiver.put("mobile",singlePrint.getReceiverPhone());
                order.put("receiver",receiver);
                // 商品信息集合
                List<Object> items = new ArrayList<>();
                // 单个商品列表
                JSONObject item = new JSONObject();
                // 商品名称
                item.put("name",singlePrint.getItemName());
                // 商品数量
                item.put("number",singlePrint.getItemNum().toString());
                // 说明
                item.put("remark",singlePrint.getItemRemark());
                items.add(item);
                order.put("items",items);
                // 运单类型，可固定为common
                order.put("order_type","common");
                // 350（默认）
                order.put("node_id","350");
                orders.add(order);
                jsonObject.put("orders",orders);
                String res = DllInitializer.ydCreateBmOrder(jsonObject.toString(),"004064","eed7ae222b8541deae79cdfc318b7aa8");
                Map resMap = JsonUtil.transferToObj(res,Map.class);
                if (resMap.get("code").equals("0000")){
                    List dataList = (List) resMap.get("data");
                    Map data = (Map) dataList.get(0);
                    waybillCode = data.get("mail_no").toString();
                    result.put("code","200");
                    result.put("msg","获取快递订单成功");
                    result.put("data",resMap.get("data"));
                }else{
                    result.put("code","500");
                    result.put("msg",resMap.get("data"));
                }
            }
        }else if (fastMailType.equals("2")){
            // 入参信息
            JSONObject paramWaybillCloudPrintApplyNewRequest = new JSONObject();
            /**
             * 发货人信息
             */
            JSONObject sender = new JSONObject();
            // 发货地址，需要入参与 search 接口中的发货人地址信息一致
            JSONObject address = new JSONObject();
            address.put("province",senderProvince);       // 省
            address.put("city",senderCity);               // 市
            address.put("district",senderDistrict);       // 区
            address.put("detail",senderDetail);           // 详细地址
            sender.put("address",address);
            // 发货人
            sender.put("name",singlePrint.getSenderName());
            // 发货人 - 手机号码
            sender.put("mobile",singlePrint.getSenderPhone());
            // 发货人 - 固定电话
            sender.put("phone",singlePrint.getSenderPhone());
            /**
             * 取号列表
             */
            List<JSONObject> tradeOrderInfoDtos = new ArrayList<>();
            // 取号对象
            JSONObject tradeOrderInfoDto = new JSONObject();
            // 请求id
            tradeOrderInfoDto.put("object_id",orderSn);
            // 订单信息
            JSONObject orderInfo = new JSONObject();
            // 订单渠道平台编码
            orderInfo.put("order_channels_type","OTHERS");
            // 订单号 数量限制100
            List<String> tradeOrderList = new ArrayList<>();
            // 订单号
            tradeOrderList.add(orderSn);
            orderInfo.put("trade_order_list",tradeOrderList);
            // 包裹信息
            JSONObject packageInfo = new JSONObject();
            List<JSONObject> items = new ArrayList<>();
            // 单个商品列表
            JSONObject item = new JSONObject();
            // 商品名称
            item.put("name",singlePrint.getItemName());
            // 商品数量
            item.put("count",singlePrint.getItemNum().toString());
            items.add(item);
            packageInfo.put("items",items);
            tradeOrderInfoDto.put("order_info",orderInfo);
            tradeOrderInfoDto.put("package_info",packageInfo);
            // 收件人信息
            JSONObject recipient = new JSONObject();
            // 收件人地址
            JSONObject recipientAddress = new JSONObject();
            // 省
            recipientAddress.put("province",map.get("province").toString());
            // 市
            recipientAddress.put("city",map.get("city").toString());
            // 区
            recipientAddress.put("district",map.get("district").toString());
            // 详细地址
            recipientAddress.put("detail",map.get("detail").toString());
            recipient.put("address",recipientAddress);
            // 手机号
            recipient.put("mobile",singlePrint.getReceiverPhone());
            // 收件人姓名
            recipient.put("name",singlePrint.getReceiverName());
            tradeOrderInfoDto.put("recipient",recipient);
            // 标准模板模板URL
            tradeOrderInfoDto.put("template_url", PddUtil.getCloudprintStdtemplates(wpCode));
            // 使用者ID
            tradeOrderInfoDto.put("user_id",remarkData.get("mallId").toString());
            tradeOrderInfoDtos.add(tradeOrderInfoDto);
            paramWaybillCloudPrintApplyNewRequest.put("sender",sender);
            paramWaybillCloudPrintApplyNewRequest.put("trade_order_info_dtos",tradeOrderInfoDtos);
            paramWaybillCloudPrintApplyNewRequest.put("wp_code",wpCode);
            String json = paramWaybillCloudPrintApplyNewRequest.toString();
            String res = PddSimpleDllLoader.executePddApi("PddWaybillGet", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,remarkData.get("token").toString(), json);
            Map resMap = JsonUtil.transferToObj(res,Map.class);
            Map pddWaybillGetResponse = (Map)resMap.get("pdd_waybill_get_response");
            List modules = (List)pddWaybillGetResponse.get("modules");
            Map module = (Map)modules.get(0);

            List goodsList = new ArrayList();
            Map goods = new HashMap();
            goods.put("goodsName",singlePrint.getItemName());
            goods.put("goodsCount",singlePrint.getItemNum());
            goodsList.add(goods);
            module.put("dataList",goodsList);
            // 运单号
            waybillCode = module.get("waybill_code").toString();
            result.put("code","200");
            result.put("msg","获取快递订单成功");
            result.put("data",module);
        }

        if (result.get("code").equals("200") && !StringUtils.isEmpty(waybillCode)){
            singlePrint.setMailNo(waybillCode);
            singlePrint.setOrderNo(orderSn);
            singlePrint.setCreateAt(System.currentTimeMillis());
            singlePrintService.save(singlePrint);
            result.put("singlePrintId",singlePrint.getId());
            result.put("fastMailType",fastMailType);
        }else{
            result.put("code","500");
            result.put("msg","获取快递订单异常");
        }
        return result;
    }


    @GetMapping("/printOne")
    public Map printOne(String id){
        // 返回值对象定义
        Map result = new HashMap();
        SinglePrint singlePrint = singlePrintService.getById(Long.parseLong(id));
        Map fastMailVo = JsonUtil.transferToObj(singlePrint.getFastMailText(),Map.class);
        // 快递账号类型  1 网点  2 拼多多
        String fastMailType = fastMailVo.get("fastMailType").toString();

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
            order.put("mailno", singlePrint.getMailNo());
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
                result.put("mailNo",singlePrint.getMailNo());
                result.put("fastMailType",fastMailType);
                List list = new ArrayList();
                Map goodsMap = new HashMap();
                goodsMap.put("goodsName",singlePrint.getItemName());
                goodsMap.put("goodsCount",singlePrint.getItemNum());
                list.add(goodsMap);
                result.put("dataList",list);
                return result;
            }
            result.put("code","500");
            result.put("msg",resMap.get("message").toString());
            return result;
        }else if(fastMailType.equals("2")){
            // 拼多多打印

        }

        return new HashMap();
    }


    /**
     * 电子面单取消
     * @param id
     * @return
     */
    @GetMapping("/cancelBmOrder")
    public Map cancelBmOrder(String id){
        // 返回值对象定义
        Map result = new HashMap();
        SinglePrint singlePrint = singlePrintService.getById(Long.parseLong(id));
        Map fastMailVo = JsonUtil.transferToObj(singlePrint.getFastMailText(),Map.class);
        // 快递账号类型  1 网点  2 拼多多
        String fastMailType = fastMailVo.get("fastMailType").toString();
        if (fastMailType.equals("1")){
            // 参数定义
            Map params = new HashMap();
            //也是app-key
            params.put("appid","004064");
            params.put("partner_id",fastMailVo.get("partnerId").toString());
            params.put("secret",fastMailVo.get("secret").toString());
            List<Map> orders = new ArrayList();
            Map order = new HashMap();
            order.put("order_serial_no", singlePrint.getOrderNo());
            order.put("mailno", singlePrint.getMailNo());
            orders.add(order);
            params.put("orders",orders);
            String jsonData = JsonUtil.transferToJson(params);
            String res = DllInitializer.ydCancelBmOrder(jsonData, "004064", "eed7ae222b8541deae79cdfc318b7aa8");
            Map resMap = JsonUtil.transferToObj(res,Map.class);
            if (resMap.get("code").equals("0000") && resMap.get("message").equals("请求成功")){
                List dataList = (List) resMap.get("data");
                Map data = (Map) dataList.get(0);
                if (data.get("msg").toString().contains("ERROR")){
                    result.put("code","500");
                }else{
                    singlePrint.setStatus("2");
                    singlePrintService.update(singlePrint);
                    result.put("code","200");
                }
                result.put("msg",data.get("msg").toString());
                return result;
            }
            result.put("code","500");
            result.put("msg",resMap.get("message").toString());
            return result;
        }else if (fastMailType.equals("2")){
            // 快递公司编码
            String wpCode = fastMailVo.get("type").toString();
            // 发货地址信息
            Map remarkData = JsonUtil.transferToObj(fastMailVo.get("remark").toString(),Map.class);
            JSONObject jsonObject = new JSONObject();
            // 运单号
            jsonObject.put("waybill_code",singlePrint.getMailNo());
            // 快递公司code
            jsonObject.put("wp_code",wpCode);
            String res = PddSimpleDllLoader.executePddApi("PddWaybillCancel", PddUtil.CLIENT_ID,PddUtil.CLIENT_SECRET,remarkData.get("token").toString(), jsonObject.toString());
            Map resMap = JsonUtil.transferToObj(res,Map.class);
            if ((Boolean) resMap.get("success")){
                singlePrint.setStatus("2");
                singlePrintService.update(singlePrint);
                result.put("code","200");
                result.put("msg","取消成功");
            }else {
                result.put("code","500");
                result.put("msg",resMap.get("message").toString());
            }
        }
        return result;
    }

    /**
     * 更新
     */
    @PostMapping("/edit")
    public Boolean update(@RequestBody SinglePrint singlePrint) {
        return singlePrintService.update(singlePrint);
    }

    /**
     * 删除
     */
    @PostMapping("/deleteById/{id}")
    public Boolean delete(@PathVariable Long id) {
        return singlePrintService.deleteById(id);
    }

    /**
     * 根据快递单号删除
     */
    @PostMapping("/deleteByMailNo/{mailNo}")
    public Boolean deleteByMailNo(@PathVariable String mailNo) {
        return singlePrintService.deleteByMailNo(mailNo);
    }

    /**
     * 批量删除
     */
    @PostMapping("/deleteBatch")
    public Boolean deleteBatch(@RequestBody List<Long> ids) {
        return singlePrintService.deleteBatch(ids);
    }
}