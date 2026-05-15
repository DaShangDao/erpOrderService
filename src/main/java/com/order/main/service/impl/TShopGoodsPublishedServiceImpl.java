package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.dto.GoodsDto;
import com.order.main.dto.TShopGoodsPublishedDto;
import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.Shop;
import com.order.main.mapper.TShopGoodsPublishedMapper;
import com.order.main.service.IShopService;
import com.order.main.service.TShopGoodsPublishedService;
import com.order.main.util.InterfaceUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 已发布商品信息Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TShopGoodsPublishedServiceImpl implements TShopGoodsPublishedService {

    private final TShopGoodsPublishedMapper tShopGoodsPublishedMapper;

    private final IShopService shopService;

    @Override
    @DS("taskDb")
    public TShopGoodsPublishedDto selectById(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return tShopGoodsPublishedMapper.selectById(id);
        } catch (Exception e) {
            log.error("根据ID查询已发布商品失败, id: {}", id, e);
            return null;
        }
    }

    /**
     * 根据平台商品id查询
     * @param trilateralId
     * @return
     */
    @Override
    @DS("taskDb")
    public List<TShopGoodsPublishedDto> selectByTrilateralId(Long trilateralId){
        return tShopGoodsPublishedMapper.selectByTrilateralId(trilateralId);
    }

    @Override
    public void createSalesOrder(String orderId,String orderSn,String productId,String unitPrice,
                                 String quantity,String sales_person,
                                 String sales_person_id,String about_id,String shopType,String receiverName,String receiverPhone,String receiverAddress) {


        Map<String, String> requestParams = new HashMap<>();
        // 添加签名相关参数
        requestParams.put("app_key", "psi");
        requestParams.put("client_id", "psi");
        requestParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        requestParams.put("sign_method", "md5");
        // 添加业务参数（从对象中获取）
        //关联订单id（平台ID）
        requestParams.put("association_order_id",orderId);
        // 订单编号
        requestParams.put("association_order_no", orderSn);
        // 来源类型 0-预留 1-erp订单
        requestParams.put("from_type", "1");
        // 商品id
        requestParams.put("items[0][product_id]",productId);
        // 单价
        requestParams.put("items[0][unit_price]", unitPrice);
        // 数量
        requestParams.put("items[0][quantity]", quantity);
        // 店铺名
        requestParams.put("sales_person", sales_person);
        // 店铺id
        requestParams.put("sales_person_id", sales_person_id);
        // 店铺创建id
        requestParams.put("about_id", about_id);
        // 店铺类型
        requestParams.put("shop_type",shopType);
        // 收货人姓名
        requestParams.put("receiver_name",receiverName);
        // 收货人电话
        requestParams.put("receiver_phone",receiverPhone);
        // 收货地址
        requestParams.put("receiver_address",receiverAddress);

        // 调用远程接口
        String result = InterfaceUtils.postFormWithSign(
                "http://192.168.101.213:9090/api/sales-order/create",
                null,
                requestParams,
                "",
                "md5"
        );
        Map resultMap = JsonUtil.transferToObj(result, Map.class);
        System.out.println(resultMap);
    }
}