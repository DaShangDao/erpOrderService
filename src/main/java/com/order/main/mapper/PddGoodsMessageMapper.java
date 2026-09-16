package com.order.main.mapper;

import com.order.main.entity.PddGoodsMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 拼多多商品类消息中转 Mapper
 *
 * @author yxy
 * @date 2026-08-04
 */
@Mapper
public interface PddGoodsMessageMapper {

    /**
     * 新增（WebSocket 收到消息后入库，毫秒级）
     */
    int insert(PddGoodsMessage message);

    /**
     * 查询待处理消息（最旧的优先，最多 limit 条）
     */
    List<PddGoodsMessage> selectPending(@Param("limit") int limit);

    /**
     * 根据ID删除（先删后处理，防宕机重复消费）
     */
    int deleteById(Long id);
}
