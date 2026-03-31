package com.order.main.mapper;

import com.order.main.entity.SinglePrint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SinglePrintMapper {

    /**
     * 根据ID查询
     */
    SinglePrint selectById(Long id);

    /**
     * 根据快递单号查询
     * @param mailNo 快递单号
     * @return
     */
    SinglePrint selectByMailNo(String mailNo);

    /**
     * 分页查询列表
     */
    List<SinglePrint> selectPageList(SinglePrint singlePrint);

    /**
     * 查询总记录数
     */
    int count(SinglePrint singlePrint);

    /**
     * 条件查询列表
     */
    List<SinglePrint> selectList(SinglePrint query);

    /**
     * 新增
     */
    int insert(SinglePrint singlePrint);

    /**
     * 更新
     */
    int update(SinglePrint singlePrint);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 根据快递单号删除
     * @param mailNo
     * @return
     */
    int deleteByMailNo(String mailNo);

    /**
     * 批量删除
     */
    int deleteBatch(@Param("ids") List<Long> ids);
}