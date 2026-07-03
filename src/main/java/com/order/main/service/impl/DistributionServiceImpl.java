package com.order.main.service.impl;

import com.order.main.dto.GoodsDto;
import com.order.main.dto.TShopGoodsPublishedDto;
import com.order.main.entity.*;
import com.order.main.service.IPsiEmployeesService;
import com.order.main.service.ISysUserService;
import com.order.main.util.InterfaceUtils;
import com.order.main.util.MaskUtils;
import com.pdd.pop.sdk.common.util.JsonUtil;
import com.pdd.pop.sdk.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 分销服务层
 */
@Service
@RequiredArgsConstructor
public class DistributionServiceImpl {

    private final ISysUserService userService;
    private final IPsiEmployeesService psiEmployeesService;

}
