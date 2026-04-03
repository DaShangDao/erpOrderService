package com.order.main.service;

import com.order.main.entity.SinglePrint;

import java.util.List;
import java.util.Map;

public interface ISinglePrintService {

    /**
     * 根据ID查询单票打印记录
     */
    SinglePrint getById(Long id);

    /**
     * 根据快递单号查询
     * @param mailNo
     * @return
     */
    SinglePrint getByMailNo(String mailNo);

    /**
     * 分页查询列表
     */
    List<SinglePrint> getPageList(SinglePrint singlePrint);

    /**
     * 查询总记录数（用于分页）
     */
    int count(SinglePrint singlePrint);

    /**
     * 查询所有列表
     */
    List<SinglePrint> getList(SinglePrint query);

    /**
     * 新增
     */
    boolean save(SinglePrint singlePrint);

    /**
     * 更新
     */
    boolean update(SinglePrint singlePrint);

    /**
     * 根据ID删除
     */
    boolean deleteById(Long id);

    /**
     * 根据快递单号删除
     * @param mailNo
     * @return
     */
    boolean deleteByMailNo(String mailNo);

    /**
     * 批量删除
     */
    boolean deleteBatch(List<Long> ids);

    /**
     * 打印
     */
    Map printView(Map fastMailVo,String mailNo,String orderNo,List itemList);
}