package com.order.main.service;

import com.order.main.entity.PsiEmployees;

/**
 * psi用户列表
 */
public interface IPsiEmployeesService {


    /**
     * 根据about和电话号查询用户和分账配置
     * @param aboutId
     * @param phone
     * @return
     */
    PsiEmployees selectOneByAboutIdAndPhone(Long aboutId, String phone);
}
