package com.order.main.controller;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.DllInitializer;
import com.order.main.dll.PddSimpleDllLoader;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.*;
import com.order.main.service.*;
import com.order.main.util.PddUtil;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/singlePrint")
public class SinglePrintController {

    private final ISinglePrintService singlePrintService;
    private final IZtoPrintService ztoPrintService;
    private final IEmsPrintService emsPrintService;
    private final IJtPrintService jtPrintService;
    private final IYtoPrintService ytoPrintService;
    private final IExpressDeliveryOrderService expressDeliveryOrderService;

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
        // 快递类型
        String type = fastMailVo.get("type").toString();
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

        if (type.equals("YZXB") || type.equals("JTSD") || type.equals("YTO")){
            // 寄件人信息
            Sender sender = new Sender();
            // 名称
            sender.setName(logisticsVo.get("contact").toString());
            // 电话
            sender.setPhone(logisticsVo.get("phoneNumber").toString());
            // 座机
            sender.setMobile(logisticsVo.get("phoneNumber").toString());
            // 省
            sender.setProv(senderProvince);
            // 市
            sender.setCity(senderCity);
            // 区
            sender.setCounty(senderDistrict);
            // 详细地址
            sender.setAddress(senderDetail);
            // 收件人信息
            Receiver receiver = new Receiver();
            // 名称
            receiver.setName(map.get("name").toString());
            // 电话
            receiver.setPhone(map.get("phone").toString());
            // 座机
            receiver.setMobile(map.get("phone").toString());
            // 省
            receiver.setProv(map.get("province").toString());
            // 市
            receiver.setCity(map.get("city").toString());
            // 区
            receiver.setCounty(map.get("district").toString());
            // 详细地址
            receiver.setAddress(map.get("detail").toString());
            // 订单信息
            ErpGoodsOrder erpGoodsOrder = new ErpGoodsOrder();
            erpGoodsOrder.setCreatedBy(Long.parseLong(fastMailVo.get("createBy").toString()));
            erpGoodsOrder.setOrderSn(orderSn);
            // 商品信息
            List<Item> itemList = new ArrayList<>();
            // 单个商品列表
            Item item = new Item();
            // 商品名称
            item.setName(map.get("itemName").toString());
            // 商品数量
            item.setNum(map.get("itemNum").toString());
            itemList.add(item);
            // 数据库存储数据
            ExpressDeliveryOrder expressDeliveryOrder = new ExpressDeliveryOrder();
            expressDeliveryOrder.setLogisticsOrderNo(erpGoodsOrder.getOrderSn());
            expressDeliveryOrder.setSenderStr(JsonUtil.transferToJson(sender));
            expressDeliveryOrder.setReceiverStr(JsonUtil.transferToJson(receiver));
            expressDeliveryOrder.setItemStr(JsonUtil.transferToJson(itemList));
            expressDeliveryOrder.setType(type);
            expressDeliveryOrder.setFastMailStr(map.get("fastMailVo").toString());
            if (type.equals("YZXB")){
                String resData = emsPrintService.createOrder(erpGoodsOrder,receiver,sender,itemList,fastMailVo.get("partnerId").toString(),fastMailVo.get("secret").toString(),fastMailVo.get("remark").toString());
                // 转义
                Map resDataMap = JsonUtil.transferToObj(resData,Map.class);
                if (resDataMap.get("retCode").toString().equals("00000")){
                    // 创建成功
                    Map retBody = JsonUtil.transferToObj(resDataMap.get("retBody").toString(),Map.class);
                    // 快递号
                    expressDeliveryOrder.setWaybillNo(retBody.get("waybillNo").toString());
                    // 大头笔编码
                    expressDeliveryOrder.setMarkDestinationCode(retBody.get("markDestinationCode").toString());
                    // 大头笔名称
                    expressDeliveryOrder.setMarkDestinationName(retBody.get("markDestinationName").toString());
                    // 集包地编码
                    expressDeliveryOrder.setPackageCode(retBody.get("packageCode").toString());
                    // 集包地名称
                    expressDeliveryOrder.setPackageCodeName(retBody.get("packageCodeName").toString());
                    // 1 创建成功  2 已回收
                    expressDeliveryOrder.setStatus("1");
                    // 新增数据库
                    expressDeliveryOrderService.save(expressDeliveryOrder);
                    result.put("code","200");
                    result.put("msg","创建成功");
                    // 订单信息
                    result.put("expressDeliveryOrder",expressDeliveryOrder);
                    // 回填快递单号
                    waybillCode = expressDeliveryOrder.getWaybillNo();
                }else{
                    try{
                        result.put("code","500");
                        result.put("msg","创建失败："+resDataMap.get("retMsg").toString());
                    }catch (Exception e){
                        result.put("code","500");
                        result.put("msg","创建失败："+resDataMap);
                    }
                }
            }else if(type.equals("JTSD")){
                // 极兔快递
                String resData = jtPrintService.createOrder(erpGoodsOrder,receiver,sender,itemList,fastMailVo.get("partnerId").toString(),fastMailVo.get("secret").toString());
                // 转义
                Map resDataMap = JsonUtil.transferToObj(resData,Map.class);
                if (resDataMap.get("code").equals("1") && resDataMap.get("msg").equals("success")){
                    // 创建成功
                    Map data = (Map) resDataMap.get("data");
                    // 快递号
                    expressDeliveryOrder.setWaybillNo(data.get("billCode").toString());
                    // 大头笔名称
                    expressDeliveryOrder.setMarkDestinationName(data.get("sortingCode").toString());
                    // 集包地名称
                    expressDeliveryOrder.setPackageCodeName(data.get("lastCenterName").toString());
                    // 1 创建成功  2 已回收
                    expressDeliveryOrder.setStatus("1");
                    // 新增数据库
                    expressDeliveryOrderService.save(expressDeliveryOrder);
                    result.put("code","200");
                    result.put("msg","创建成功");
                    // 订单信息
                    result.put("expressDeliveryOrder",expressDeliveryOrder);
                    // 回填快递单号
                    waybillCode = expressDeliveryOrder.getWaybillNo();
                }else{
                    try{
                        result.put("code","500");
                        result.put("msg","创建失败："+resDataMap.get("msg"));
                    }catch (Exception e){
                        result.put("code","500");
                        result.put("msg","创建失败："+resDataMap);
                    }
                }
            }else if(type.equals("YTO")){
                // 圆通
                String resData =  ytoPrintService.createOrder(erpGoodsOrder,receiver,sender,itemList,fastMailVo.get("partnerId").toString(),fastMailVo.get("secret").toString());
                Map resDataMap = JsonUtil.transferToObj(resData,Map.class);
                if (resDataMap.get("mailNo") != null){
                    // 快递号
                    expressDeliveryOrder.setWaybillNo(resDataMap.get("mailNo").toString());
                    // 大头笔名称
                    expressDeliveryOrder.setMarkDestinationName(resDataMap.get("shortAddress").toString());
                    // 1 创建成功  2 已回收
                    expressDeliveryOrder.setStatus("1");
                    // 新增数据库
                    expressDeliveryOrderService.save(expressDeliveryOrder);
                    result.put("code","200");
                    result.put("msg","创建成功");
                    // 订单信息
                    result.put("expressDeliveryOrder",expressDeliveryOrder);
                    // 回填快递单号
                    waybillCode = expressDeliveryOrder.getWaybillNo();
                }else{
                    try{
                        result.put("code","500");
                        result.put("msg","创建失败："+resDataMap.get("reaspm"));
                    }catch (Exception e){
                        result.put("code","500");
                        result.put("msg","创建失败："+resDataMap);
                    }
                }
                System.out.println(resData);
            }else{
                result.put("code","500");
                result.put("msg","异常快递类型"+type);
            }
        }else{
            if (fastMailType.equals("1")){
                if (type.equals("YUNDA")){
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
                }else if (type.equals("ZTO")){
                    JSONObject jsonObject = new JSONObject();
                    // 合作模式 ，1：集团客户；2：非集团客户
                    jsonObject.put("partnerType","2");
                    // partnerType为1时，orderType：1：全网件 2：预约件。 partnerType为2时，orderType：1：全网件 3：预约件（不返回运单号）
                    jsonObject.put("orderType","1");
                    // 合作商订单号
                    jsonObject.put("partnerOrderCode",orderSn);
                    // 账号信息 ,AccountDto
                    JSONObject accountInfo = new JSONObject();
                    // 电子面单账号（partnerType为2，orderType传1,2,4时必传）
                    accountInfo.put("accountId",fastMailVo.get("partnerId").toString());
                    // 电子面单密码（测试环境传ZTO123）
                    accountInfo.put("accountPassword",fastMailVo.get("secret").toString());
                    // 单号类型:1.普通电子面单；74.星联电子面单；默认是1
                    accountInfo.put("type","1");
                    jsonObject.put("accountInfo",accountInfo);
                    // 发件人信息 ,SenderInfoInput
                    JSONObject senderInfo = new JSONObject();
                    // 发件人姓名
                    senderInfo.put("senderName",singlePrint.getSenderName());
                    // 发件人手机号
                    senderInfo.put("senderMobile",singlePrint.getSenderPhone());
                    // 发件人省
                    senderInfo.put("senderProvince",senderProvince);
                    // 发件人市
                    senderInfo.put("senderCity",senderCity);
                    // 发件人区
                    senderInfo.put("senderDistrict",senderDistrict);
                    // 发件人详细地址
                    senderInfo.put("senderAddress",singlePrint.getSenderAddress());
                    jsonObject.put("senderInfo",senderInfo);
                    // 收件人信息
                    JSONObject receiveInfo = new JSONObject();
                    // 收件人姓名
                    receiveInfo.put("receiverName",map.get("name").toString());
                    // 收件人手机号
                    receiveInfo.put("receiverMobile",map.get("phone").toString());
                    // 收件人省
                    receiveInfo.put("receiverProvince",map.get("province").toString());
                    // 收件人市
                    receiveInfo.put("receiverCity",map.get("city").toString());
                    // 收件人区
                    receiveInfo.put("receiverDistrict",map.get("district").toString());
                    // 收件人详细地址
                    receiveInfo.put("receiverAddress",map.get("detail").toString());
                    jsonObject.put("receiveInfo",receiveInfo);
                    // 物品信息
                    List<JSONObject> orderItems = new ArrayList<>();
                    // 单个商品列表
                    JSONObject orderItem = new JSONObject();
                    // 商品名称
                    orderItem.put("name",singlePrint.getItemName());
                    // 商品数量
                    orderItem.put("quantity",singlePrint.getItemNum().toString());
                    // 备注
                    orderItem.put("remark","");
                    orderItems.add(orderItem);

                    jsonObject.put("orderItems",orderItems);
                    String res = PrintSimpleDllLoader.executeZTOApi("ZtoOpenCreateOrder","6721852dd4ac4e3c30ce7 ","204b67cbbb7b1960e03e101295a0ee5e",jsonObject.toString());
                    Map resMap = JsonUtil.transferToObj(res,Map.class);
                    if (resMap.get("message").toString().equals("成功")){
                        Map resultData = (Map) resMap.get("result");
                        waybillCode = resultData.get("billCode").toString();
                        singlePrint.setRemark(res);
                        result.put("code","200");
                        result.put("msg","获取快递订单成功");
                        result.put("data",resMap);
                    }else{
                        result.put("code","500");
                        result.put("msg",resMap);
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
                paramWaybillCloudPrintApplyNewRequest.put("need_encrypt",true);
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
        }
        if (result.get("code").equals("200") && !StringUtils.isEmpty(waybillCode)){
            singlePrint.setMailNo(waybillCode);
            singlePrint.setOrderNo(orderSn);
            singlePrint.setCreateAt(System.currentTimeMillis());
            singlePrintService.save(singlePrint);
            result.put("singlePrintId",singlePrint.getId());
            result.put("fastMailType",fastMailType);
            result.put("wpCode",wpCode);
        }else if (result.get("code").equals("500") ){

        }else{
            result.put("code","500");
            result.put("msg","获取快递单异常");
        }
        return result;
    }


    @GetMapping("/printOne")
    public Map printOne(String id){
        SinglePrint singlePrint = singlePrintService.getById(Long.parseLong(id));
        List<ExpressDeliveryOrder> expressDeliveryOrderList = expressDeliveryOrderService.getByWaybillNo(singlePrint.getMailNo());
        if (!expressDeliveryOrderList.isEmpty()){
            Map resMap = new HashMap();
            resMap.put("code","200");
            resMap.put("expressDeliveryOrder",expressDeliveryOrderList.get(0));
            return resMap;
        }else{
            Map fastMailVo = JsonUtil.transferToObj(singlePrint.getFastMailText(),Map.class);

            String mailNo = singlePrint.getMailNo();
            String orderNo = singlePrint.getOrderNo();
            if(fastMailVo.get("type").equals("ZTO")){
                List itemList = new ArrayList();
                Map item = new HashMap();
                item.put("goodsName",singlePrint.getItemName());
                item.put("goodsCount",singlePrint.getItemNum());
                itemList.add(item);

                Map resultData = JsonUtil.transferToObj(singlePrint.getRemark(),Map.class);
                Map result = (Map) resultData.get("result");
                // 表头
                Map bigMarkInfo = (Map) result.get("bigMarkInfo");
                // title
                String title = bigMarkInfo.get("mark").toString();
                // 集
                String jiStr = bigMarkInfo.get("bagAddr").toString();
                JSONObject sender = new JSONObject();
                sender.put("name",singlePrint.getSenderName());
                sender.put("phone",singlePrint.getSenderPhone());
                sender.put("address",singlePrint.getSenderAddress());
                JSONObject receiver = new JSONObject();
                receiver.put("name",singlePrint.getReceiverName());
                receiver.put("phone",singlePrint.getReceiverPhone());
                receiver.put("address",singlePrint.getReceiverAddress());
                // 构建打印数据
                Map resMap = new HashMap();
                resMap.put("code","200");
                resMap.put("title",title);
                resMap.put("jiStr",jiStr);
                resMap.put("mailNo",mailNo);
                resMap.put("sender",sender);
                resMap.put("receiver",receiver);
                resMap.put("dataList",itemList);
                resMap.put("mailType",fastMailVo.get("type"));
                resMap.put("fastMailType","1");
                return resMap;
            }else{
                List itemList = new ArrayList();
                Map item = new HashMap();
                item.put("itemName",singlePrint.getItemName());
                item.put("itemNum",singlePrint.getItemNum());
                itemList.add(item);
                return singlePrintService.printView(fastMailVo,mailNo,orderNo,itemList);
            }
        }
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
        List<ExpressDeliveryOrder> expressDeliveryOrderList = expressDeliveryOrderService.getByWaybillNo(singlePrint.getMailNo());
        if (!expressDeliveryOrderList.isEmpty()){
            ExpressDeliveryOrder expressDeliveryOrder = expressDeliveryOrderList.get(0);
            Map fastMail = JsonUtil.transferToObj(expressDeliveryOrder.getFastMailStr(),Map.class);
            if (fastMail.get("type").equals("YZXB")){
                String dataStr = emsPrintService.cancelBmOrder(expressDeliveryOrder.getLogisticsOrderNo(),expressDeliveryOrder.getWaybillNo(),"1",fastMail.get("partnerId").toString(),fastMail.get("secret").toString(),fastMail.get("remark").toString());
                Map dataMap = JsonUtil.transferToObj(dataStr,Map.class);
                if (dataMap.get("retCode").toString().equals("00000")){
                    // 已回收
                    expressDeliveryOrder.setStatus("2");
                    expressDeliveryOrderService.update(expressDeliveryOrder);
                    result.put("code","200");
                    result.put("msg","回收成功");
                }else {
                    result.put("code","500");
                    result.put("msg",dataMap.get("retMsg").toString());
                }
            }else if(fastMail.get("type").equals("JTSD")){
                String dataStr = jtPrintService.jtOrderCancelOrder(fastMail.get("partnerId").toString(),fastMail.get("secret").toString(),expressDeliveryOrder.getLogisticsOrderNo(),"1");
                Map dataMap = JsonUtil.transferToObj(dataStr,Map.class);
                if (dataMap.get("code").equals("1") && dataMap.get("msg").equals("success")){
                    // 已回收
                    expressDeliveryOrder.setStatus("2");
                    expressDeliveryOrderService.update(expressDeliveryOrder);
                    result.put("code","200");
                    result.put("msg","回收成功");
                }else{
                    result.put("code","500");
                    result.put("msg",dataMap.get("retMsg").toString());
                }
                System.out.println(dataMap);
            }else if (fastMail.get("type").equals("YTO")){
                result.put("code","500");
                result.put("msg","圆通不支持回收单号，未发货运单无需回收");
            }
        }else{
            Map fastMailVo = JsonUtil.transferToObj(singlePrint.getFastMailText(),Map.class);
            // 快递账号类型  1 网点  2 拼多多
            String fastMailType = fastMailVo.get("fastMailType").toString();
            if (fastMailType.equals("1")){
                if (fastMailVo.get("type").equals("ZTO")){
                    result.put("code","500");
                    result.put("msg","中通全网件快递订单不支持回收");
                }else{
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
                }

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