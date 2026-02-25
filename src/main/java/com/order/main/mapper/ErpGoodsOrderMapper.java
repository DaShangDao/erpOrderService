package com.order.main.mapper;

import com.order.main.entity.ErpGoodsOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台订单Mapper接口
 *
 * @author yxy
 * @date 2025-12-04
 */
@Mapper
public interface ErpGoodsOrderMapper {

    /**
     * 根据ID查询ERP订单
     *
     * @param id 平台订单ID
     * @return 平台订单
     */
    ErpGoodsOrder selectById(Long id);

    /**
     * 根据订单号查询订单
     * @param orderNo
     * @return
     */
    ErpGoodsOrder selectByOrderNo(String orderNo);

    /**
     * 拼多多根据订单号获取订单列表
     * @param orderNo
     * @return
     */
    List<ErpGoodsOrder> selectListByOrderNo(String orderNo);

    /**
     * 根据订单号和商品ID查询订单
     * @param orderSn   订单号
     * @param goodsId   商品id
     * @return
     */
    ErpGoodsOrder selectBoOrderNoAndGoodsId(@Param("orderSn") String orderSn,@Param("goodsId") String goodsId);


    /**
     * 分页查询ERP订单（支持动态条件）
     *
     * @param order 查询条件对象
     * @return 订单列表
     */
    List<ErpGoodsOrder> selectPageList(@Param("order") ErpGoodsOrder order);

    /**
     * 获取ERP订单总数（支持动态条件）
     *
     * @param order 查询条件对象
     * @return 订单总数
     */
    int selectPageCount(@Param("order") ErpGoodsOrder order);

    /**
     * 插入平台订单
     *
     * @param erpGoodsOrder 平台订单
     * @return 结果
     */
    int insert(ErpGoodsOrder erpGoodsOrder);

    /**
     * 更新平台订单
     *
     * @param erpGoodsOrder 平台订单
     * @return 结果
     */
    int update(ErpGoodsOrder erpGoodsOrder);

    /**
     * 删除平台订单
     *
     * @param id 平台订单ID
     * @return 结果
     */
    int deleteById(Long id);

    /**
     * 假删除平台订单（将is_show设为1）
     *
     * @param id 平台订单ID
     * @return 结果
     */
    int fakeDeleteById(Long id);


    Integer countOrder(@Param("shopId") String shopId, @Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);
    BigDecimal todaySale(@Param("shopId") String shopId, @Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);
    Integer monthOrder(@Param("shopId") String shopId, @Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);
    BigDecimal monthSale(@Param("shopId") String shopId, @Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);

    Integer countOrderAll(@Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);
    BigDecimal todaySaleAll(@Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);
    Integer monthOrderAll(@Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);
    BigDecimal monthSaleAll(@Param("creatStart") long creatStart, @Param("creatEnd") long creatEnd);
}