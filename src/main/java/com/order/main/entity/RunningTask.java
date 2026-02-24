package com.order.main.entity;

import lombok.Data;

/**
 * 执行的任务对象 t_running_task
 *
 * @author yxy
 * @date 2025-07-23
 */
@Data
public class RunningTask {

    /**
     * 主键
     */
    private Long id;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 优先级
     */
    private Long priority;

    /**
     * 内容json字符串
     */
    private String data;

    /**
     * 状态  0 未执行  1 执行中  2 执行完成
     */
    private String status;

    /**
     * 需要被修改的状态
     */
    private String editStatus;

    /**
     * 任务执行完返回的数据
     */
    private String callBackData;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 商品id
     */
    private Long goodsId;

    /**
     * 随机数
     */
    private Long randomNum;


    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 完成数据
     */
    private String successData;

}
