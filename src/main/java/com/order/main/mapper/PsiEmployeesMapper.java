package com.order.main.mapper;

import com.order.main.entity.ExpressDeliveryOrder;
import com.order.main.entity.PsiEmployees;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PsiEmployeesMapper {

    /**
     * 根据about和电话号查询用户和分账配置
     * @param aboutId
     * @param phone
     * @return
     */
    PsiEmployees selectOneByAboutIdAndPhone(Long aboutId,String phone);

}