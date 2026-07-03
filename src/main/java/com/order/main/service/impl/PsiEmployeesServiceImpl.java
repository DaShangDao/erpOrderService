package com.order.main.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.PsiEmployees;
import com.order.main.mapper.PsiEmployeesMapper;
import com.order.main.service.IErpGoodsOrderService;
import com.order.main.service.IPsiEmployeesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * psi用户列表
 */
@Service
@RequiredArgsConstructor
public class PsiEmployeesServiceImpl implements IPsiEmployeesService {


    private final PsiEmployeesMapper psiEmployeesMapper;

    /**
     * 根据about和电话号查询用户和分账配置
     * @param aboutId
     * @param phone
     * @return
     */
    @Override
    @DS("psi")
    public PsiEmployees selectOneByAboutIdAndPhone(Long aboutId, String phone) {
        return psiEmployeesMapper.selectOneByAboutIdAndPhone(aboutId,phone);
    }
}
