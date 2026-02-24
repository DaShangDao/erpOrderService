package com.order.main.service.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.order.main.entity.Logistics;
import com.order.main.mapper.LogisticsMapper;
import com.order.main.service.LogisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class LogisticsServiceImpl implements LogisticsService {

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Override
    @DS("slave")
    public Logistics selectLogisticeByDeptId(Long deptId) {
        return logisticsMapper.selectLogisticeByDeptId(deptId);
    }
}
