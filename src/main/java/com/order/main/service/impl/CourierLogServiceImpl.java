package com.order.main.service.impl;

import com.order.main.entity.CourierLog;
import com.order.main.mapper.CourierLogMapper;
import com.order.main.service.ICourierLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierLogServiceImpl implements ICourierLogService {

    private final CourierLogMapper baseMapper;

    @Override
    public CourierLog getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<CourierLog> getListByErpOrderId(Long erpOrderId){
        return baseMapper.getListByErpOrderId(erpOrderId);
    }

    @Override
    public List<CourierLog> getPageList(CourierLog courierLog) {
        courierLog.setPageNum((courierLog.getPageNum() - 1) * courierLog.getPageSize());
        return baseMapper.selectPageList(courierLog);
    }

    @Override
    public int count(CourierLog courierLog) {
        return baseMapper.count(courierLog);
    }

    @Override
    public List<CourierLog> getList(CourierLog query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(CourierLog courierLog) {
        return baseMapper.insert(courierLog) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(CourierLog courierLog) {
        return baseMapper.update(courierLog) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据快递单号删除
     * @param mailNo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMailNo(String mailNo){
        return baseMapper.deleteByMailNo(mailNo) > 0;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Long> ids) {
        return baseMapper.deleteBatch(ids) > 0;
    }
}