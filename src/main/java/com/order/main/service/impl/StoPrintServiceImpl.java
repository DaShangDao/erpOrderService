package com.order.main.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.order.main.dll.PrintSimpleDllLoader;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Item;
import com.order.main.entity.Receiver;
import com.order.main.entity.Sender;
import com.order.main.service.IStoPrintService;
import com.order.main.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class StoPrintServiceImpl implements IStoPrintService {


    /**
     * 面单库存查询
     * @param siteCode  网点编号
     * @param userCode  客户编号
     * @param password  客户密码
     * @return
     */
    @Override
    public String getOrderStock(String siteCode,String userCode,String password){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("siteCode",siteCode);
        jsonObject.put("userCode",userCode);
        jsonObject.put("password",password);
        jsonObject.put("userType","0");
        jsonObject.put("timestamp",  System.currentTimeMillis() / 1000);
        return PrintSimpleDllLoader.exceteSTO("BILL_CODE_QUERY_USER_STOCK","CAKDVcNVcjgqoKH","CAKDVcNVcjgqoKH","3ySS5owwn6ENOSespB2c9fROojSAllA0","billcode_open",jsonObject.toString());
    }

    /**
     * 创建订单
     * @param erpGoodsOrder     订单信息
     * @param receiver          收件人
     * @param sender            寄件人
     * @param items             商品列表
     * @param siteCode          网点编号
     * @param userCode          客户编号
     * @param password          客户密码
     * @return
     */
    @Override
    public String createOrder(ErpGoodsOrder erpGoodsOrder, Receiver receiver, Sender sender, List<Item> items, String siteCode, String userCode, String password){
        JSONObject jsonObject = new JSONObject();
        // 订单号（客户系统自己生成，唯一）
        jsonObject.put("orderNo",erpGoodsOrder.getOrderSn());
        // 订单来源（订阅服务时填写的来源编码）
        jsonObject.put("orderSource","zhishuErp");
        // 获取面单的类型（00-普通、03-国际、01-代收、02-到付、04-生鲜），默认普通业务，如果有其他业务先与业务方沟通清楚
        jsonObject.put("billType","00");
        // 订单类型（01-普通订单、02-调度订单）默认01-普通订单，如果有散单业务需先业务方沟通清楚
        jsonObject.put("orderType","01");
        //寄件信息对象
        JSONObject senderObject = new JSONObject();
        // 名称
        senderObject.put("name",sender.getName());
        // 电话
        senderObject.put("mobile",sender.getPhone());
        // 省
        senderObject.put("province",sender.getProv());
        // 市
        senderObject.put("city",sender.getCity());
        // 区
        senderObject.put("area",sender.getCounty());
        // 寄件详细地址（省+市+区县+详细地址）
        senderObject.put("address",sender.getProv()+sender.getCity()+sender.getCounty()+sender.getAddress());
        jsonObject.put("sender",senderObject);
        // 收件人信息
        JSONObject receiverObject = new JSONObject();
        // 名称
        receiverObject.put("name",receiver.getName());
        // 电话
        receiverObject.put("mobile",receiver.getPhone());
        // 省
        receiverObject.put("province",receiver.getProv());
        // 市
        receiverObject.put("city",receiver.getCity());
        // 区
        receiverObject.put("area",receiver.getCounty());
        // 寄件详细地址（省+市+区县+详细地址）
        receiverObject.put("address",receiver.getProv()+receiver.getCity()+receiver.getCounty()+receiver.getAddress());
        jsonObject.put("receiver",receiverObject);
        // 包裹信息
        JSONObject cargo = new JSONObject();
        // 带电标识 （10/未知 20/带电 30/不带电）
        cargo.put("battery","10");
        // 物品类型（大件、小件、扁平件\文件）
        cargo.put("goodsType","小件");
        // 物品名称
        String goodsName = "";
        // 物品数量
        int goodsCount = 0;
        for (Item item : items){
            goodsName += item.getName()+";";
            goodsCount += Integer.parseInt(item.getNum());
        }
        // 物品名称
        cargo.put("goodsName",goodsName);
        // 物品数量
        cargo.put("goodsCount",goodsCount+"");
        jsonObject.put("cargo",cargo);
        // 客户信息，在线下单取运单号必填，代单号下单不需要填写，测试账号传值如下，生产账号联系合作业务方提供
        JSONObject customer = new JSONObject();
        customer.put("siteCode",siteCode);
        customer.put("customerName",userCode);
        customer.put("sitePwd",password);
        jsonObject.put("customer",customer);
        return PrintSimpleDllLoader.exceteSTO("OMS_EXPRESS_ORDER_CREATE","CAKDVcNVcjgqoKH","CAKDVcNVcjgqoKH","3ySS5owwn6ENOSespB2c9fROojSAllA0","sto_oms",jsonObject.toString());
    }


    /**
     * 运单号
     * @param billCode
     * @return
     */
    @Override
    public String orderCancel(String billCode){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("billCode",billCode);
        jsonObject.put("orderSource","zhishuErp");
        jsonObject.put("orderType","01");
        return PrintSimpleDllLoader.exceteSTO("EDI_MODIFY_ORDER_CANCEL","CAKDVcNVcjgqoKH","CAKDVcNVcjgqoKH","3ySS5owwn6ENOSespB2c9fROojSAllA0","edi_modify_order",jsonObject.toString());

    }

}
