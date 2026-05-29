package com.order.main.mapper;

import com.order.main.entity.ErpGoodsOrder;
import com.order.main.entity.ErpGoodsOrderQueue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ErpGoodsOrderQueueMapper {

    /**
     * 根据ID查询
     */
    ErpGoodsOrderQueue selectById(Long id);

    /**
     * 获取未下发的订单数据
     * @return
     */
    ErpGoodsOrder selectByStatus();

    /**
     * 分页查询列表
     */
    List<ErpGoodsOrderQueue> selectPageList(ErpGoodsOrderQueue queue);

    /**
     * 查询总记录数
     */
    int count(ErpGoodsOrderQueue queue);

    /**
     * 条件查询列表
     */
    List<ErpGoodsOrderQueue> selectList(ErpGoodsOrderQueue query);

    /**
     * 新增
     */
    int insert(ErpGoodsOrderQueue queue);

    /**
     * 更新
     */
    int update(ErpGoodsOrderQueue queue);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 批量删除
     */
    int deleteBatch(@Param("ids") List<Long> ids);
}