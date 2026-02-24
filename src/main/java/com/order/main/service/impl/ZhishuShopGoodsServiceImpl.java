package com.order.main.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.StockChangeLog;
import com.order.main.entity.ZhishuShopGoods;
import com.order.main.mapper.ShopMapper;
import com.order.main.mapper.ZhishuShopGoodsMapper;
import com.order.main.service.IStockChangeLogService;
import com.order.main.service.IZhishuShopGoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 商品信息Service业务层处理
 *
 */
@RequiredArgsConstructor
@Service
public class ZhishuShopGoodsServiceImpl implements IZhishuShopGoodsService {

    private final ZhishuShopGoodsMapper baseMapper;
    private final IStockChangeLogService stockChangeLogService;


    /**
     * 根据查询条件查询单挑数据
     * @param zhishuShopGoods
     * @return
     */
    @Override
    @DS("slave")
    public List<ZhishuShopGoods> selectList(ZhishuShopGoods zhishuShopGoods){
        return baseMapper.selectList(zhishuShopGoods);
    }

    /**
     * 根据id查询商品信息
     * @param id
     * @return
     */
    @Override
    @DS("slave")
    public ZhishuShopGoods selectById(Long id){
        System.out.println("查询商品参数：id:"+id);
        return baseMapper.selectById(id+"");
    }

    /**
     * 根据商品id获取运费模板
     * @param goodsId
     * @return
     */
    @Override
    @DS("slave")
    public Map selectLogisticsByGoodsId(String goodsId){
        return baseMapper.selectLogisticsByGoodsId(goodsId);
    }


    /**
     * 根据货号查询商品信息
     * @param artNo     货号
     * @return
     */
    @Override
    @DS("slave")
    public ZhishuShopGoods selectByArtNo(String artNo){
        return baseMapper.selectByArtNo(artNo);
    }

    /**
     * 修改库存
     * @param zhishuShopGoods   erp商品信息
     * @param type      1-增加库存 2-减少库存
     */
    @Override
    @DS("slave")
    public int updateInventory(ZhishuShopGoods zhishuShopGoods,String type,Long erpOrderId){
        int mark = baseMapper.updateInventory(zhishuShopGoods);
        if(mark > 0){
            //修改成功后，记录自营书品库存修改表
            StockChangeLog stockChangeLog = new StockChangeLog();
            stockChangeLog.setShopGoodsId(Long.parseLong(zhishuShopGoods.getId()));
            //类型  2-订单减扣 3-退单添加
            stockChangeLog.setType(type.equals("1") ? 3 : 2);
            stockChangeLog.setAboutId(erpOrderId+"");
            stockChangeLog.setBeforeInv(zhishuShopGoods.getOldInventory());
            stockChangeLog.setAfterInv(zhishuShopGoods.getInventory());
            stockChangeLog.setCreateBy(zhishuShopGoods.getUserId());
            stockChangeLog.setUpdateBy(zhishuShopGoods.getUserId());
            stockChangeLogService.insert(stockChangeLog);
        }
        return mark;
    }
}




