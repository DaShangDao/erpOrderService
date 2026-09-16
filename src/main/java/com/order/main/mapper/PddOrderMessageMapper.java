package com.order.main.mapper;

import com.order.main.entity.PddOrderMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 拼多多订单类消息队列 Mapper
 *
 * @author yxy
 * @date 2026-08-04
 */
@Mapper
public interface PddOrderMessageMapper {

    /**
     * 新增（WebSocket 收到消息后入库，毫秒级）
     */
    int insert(PddOrderMessage message);

    /**
     * 查询待处理消息（最旧的优先，最多 limit 条）
     */
    List<PddOrderMessage> selectPending(@Param("limit") int limit);

    /**
     * 更新状态（成功/失败/终态）
     */
    int updateStatus(PddOrderMessage message);
}
