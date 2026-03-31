package com.order.main.service.impl;

import com.order.main.entity.SinglePrint;
import com.order.main.mapper.SinglePrintMapper;
import com.order.main.service.ISinglePrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SinglePrintServiceImpl implements ISinglePrintService {

    private final SinglePrintMapper baseMapper;

    @Override
    public SinglePrint getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public SinglePrint getByMailNo(String mailNo) {
        return baseMapper.selectByMailNo(mailNo);
    }

    @Override
    public List<SinglePrint> getPageList(SinglePrint singlePrint) {
        singlePrint.setPageNum((singlePrint.getPageNum() - 1) * singlePrint.getPageSize());
        return baseMapper.selectPageList(singlePrint);
    }

    @Override
    public int count(SinglePrint singlePrint) {
        return baseMapper.count(singlePrint);
    }

    @Override
    public List<SinglePrint> getList(SinglePrint query) {
        return baseMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SinglePrint singlePrint) {
        return baseMapper.insert(singlePrint) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SinglePrint singlePrint) {
        return baseMapper.update(singlePrint) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByMailNo(String mailNo) {
        return baseMapper.deleteByMailNo(mailNo) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBatch(List<Long> ids) {
        return baseMapper.deleteBatch(ids) > 0;
    }
}