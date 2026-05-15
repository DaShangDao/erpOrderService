package com.order.main.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.dto.GoodsDto;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.service.IZtoPrintService;
import com.order.main.util.DateUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ZtoPrintServiceImpl implements IZtoPrintService {


    /**
     * 电子面单绑定
     * @param account
     * @param password
     * @param dataMap
     * @return
     */
    @Override
    public String bindingEaccount(String account, String password, Map dataMap) {
        // 参数定义
        JSONObject params = new JSONObject();
        params.put("eaccount",account);
        params.put("eaccountPwd",password);
        params.put("siteCode",dataMap.get("siteCode").toString());
        return PrintSimpleDllLoader.executeZTOApi("ZtoOpenBindingEaccount","6721852dd4ac4e3c30ce7 ","204b67cbbb7b1960e03e101295a0ee5e",params.toString());
    }

    /**
     * 获取快递余额
     * @param account
     * @param password
     * @param dataMap
     * @return
     */

    @Override
    public String faceSheetBalance(String account, String password, Map dataMap) {
        JSONObject params = new JSONObject();
        params.put("datetime", DateUtils.getTimeByDayOffset(0));
        params.put("partner",account);
        params.put("verify",password);
        JSONObject content = new JSONObject();
        content.put("typeId","1");
        params.put("content",content);
        return PrintSimpleDllLoader.executeZTOApi("ZtoOpenQueryAvailableBalanceNew","6721852dd4ac4e3c30ce7 ","204b67cbbb7b1960e03e101295a0ee5e",params.toString());
    }

    /**
     * 创建订单
     * @param erpGoodsOrderList
     * @param logisticsMap
     * @return
     */
    @Override
    public String createOrder(String accountId,String accountPassword,List<ErpGoodsOrder> erpGoodsOrderList, Map logisticsMap) {
        // 联系人/发货人
        String senderName = logisticsMap.get("contact").toString();
        // 联系电话
        String phoneNumber = logisticsMap.get("phone_number").toString();
        // 发货省
        String deliveryProvince = logisticsMap.get("delivery_province").toString();
        // 发货市
        String deliveryCity = logisticsMap.get("delivery_city").toString();
        // 发货区
        String deliveryArea = logisticsMap.get("delivery_area").toString();

        ErpGoodsOrder erpGoodsOrder = erpGoodsOrderList.get(0);
        JSONObject jsonObject = new JSONObject();
        // 合作模式 ，1：集团客户；2：非集团客户
        jsonObject.put("partnerType","2");
        // partnerType为1时，orderType：1：全网件 2：预约件。 partnerType为2时，orderType：1：全网件 3：预约件（不返回运单号）
        jsonObject.put("orderType","1");
        // 合作商订单号
        jsonObject.put("partnerOrderCode",erpGoodsOrder.getOrderSn());
        // 账号信息 ,AccountDto
        JSONObject accountInfo = new JSONObject();
        // 电子面单账号（partnerType为2，orderType传1,2,4时必传）
        accountInfo.put("accountId",accountId);
        // 电子面单密码（测试环境传ZTO123）
        accountInfo.put("accountPassword",accountPassword);
        // 单号类型:1.普通电子面单；74.星联电子面单；默认是1
        accountInfo.put("type","1");
        jsonObject.put("accountInfo",accountInfo);
        // 发件人信息 ,SenderInfoInput
        JSONObject senderInfo = new JSONObject();
        // 发件人姓名
        senderInfo.put("senderName",senderName);
        // 发件人手机号
        senderInfo.put("senderMobile",phoneNumber);
        // 发件人省
        senderInfo.put("senderProvince",deliveryProvince);
        // 发件人市
        senderInfo.put("senderCity",deliveryCity);
        // 发件人区
        senderInfo.put("senderDistrict",deliveryArea);
        // 发件人详细地址
        senderInfo.put("senderAddress",deliveryProvince+","+deliveryCity+","+deliveryArea);
        jsonObject.put("senderInfo",senderInfo);
        // 收件人信息
        JSONObject receiveInfo = new JSONObject();
        // 收件人姓名
        receiveInfo.put("receiverName",erpGoodsOrder.getReceiverName());
        // 收件人手机号
        receiveInfo.put("receiverMobile",erpGoodsOrder.getMobile());
        // 收件人省
        receiveInfo.put("receiverProvince",erpGoodsOrder.getProvince());
        // 收件人市
        receiveInfo.put("receiverCity",erpGoodsOrder.getCity());
        // 收件人区
        receiveInfo.put("receiverDistrict",erpGoodsOrder.getCountry());
        // 收件人详细地址
        receiveInfo.put("receiverAddress",erpGoodsOrder.getTown());
        jsonObject.put("receiveInfo",receiveInfo);
        // 物品信息
        List<JSONObject> orderItems = new ArrayList<>();
        for (ErpGoodsOrder ego : erpGoodsOrderList){
            GoodsDto goodsDto = JsonUtil.transferToObj(ego.getItemList(),GoodsDto.class);
            // 单个商品列表
            JSONObject orderItem = new JSONObject();
            // 商品名称
            orderItem.put("name",goodsDto.getGoodsName());
            // 商品数量
            orderItem.put("quantity",goodsDto.getGoodsCount());
            // 备注
            orderItem.put("remark","");
            orderItems.add(orderItem);
        }
        jsonObject.put("orderItems",orderItems);
        return PrintSimpleDllLoader.executeZTOApi("ZtoOpenCreateOrder","6721852dd4ac4e3c30ce7 ","204b67cbbb7b1960e03e101295a0ee5e",jsonObject.toString());
    }

    /**
     * 取消订单
     * @param cancelType
     * @param billCode
     * @return
     */
    @Override
    public String cancelPreOrder(String cancelType, String billCode) {
        JSONObject jsonObject = new JSONObject();
        //取消类型 1不想寄了,2下错单,3重复下单,4运费太贵,5无人联系,6取件太慢,7态度差
        jsonObject.put("cancelType",cancelType);
        // 运单号
        jsonObject.put("billCode",billCode);
        return PrintSimpleDllLoader.executeZTOApi("ZtoOpenCancelPreOrder","6721852dd4ac4e3c30ce7 ","204b67cbbb7b1960e03e101295a0ee5e",jsonObject.toString());

    }

    /**
     * 请求生成面单图片/PDF
     * @param billCode
     * @return
     */
    @Override
    public String orderPrint(String billCode) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("printType","1");
        // 运单号
        jsonObject.put("billCode",billCode);
        return PrintSimpleDllLoader.executeZTOApi("ZtoOpenOrderPrint","6721852dd4ac4e3c30ce7 ","204b67cbbb7b1960e03e101295a0ee5e",jsonObject.toString());

    }

    @Override
    public String getOrderInfo(String type, String billCode) {
        JSONObject jsonObject = new JSONObject();
        //0，预约件 1，全网件
        jsonObject.put("type",type);
        // 运单号
        jsonObject.put("billCode",billCode);
        return PrintSimpleDllLoader.executeZTOApi("ZtoOpenGetOrderInfo","6721852dd4ac4e3c30ce7 ","204b67cbbb7b1960e03e101295a0ee5e",jsonObject.toString());
    }


}
