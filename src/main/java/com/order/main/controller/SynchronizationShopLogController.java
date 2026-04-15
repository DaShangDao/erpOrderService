package com.order.main.controller;

import com.order.main.entity.SynchronizationShopLog;
import com.order.main.service.ISynchronizationShopLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/synchronizationShopLog")
public class SynchronizationShopLogController {

    private final ISynchronizationShopLogService synchronizationShopLogService;

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public SynchronizationShopLog getById(@PathVariable Long id) {
        return synchronizationShopLogService.getById(id);
    }

    /**
     * 分页查询列表
     */
    @GetMapping("/getList")
    public Map<String, Object> getList(SynchronizationShopLog log) {
        // 对 shopName 进行 URL 解码
        if (log.getShopName() != null && log.getShopName().trim().length() > 0) {
            String decodedShopName = URLDecoder.decode(log.getShopName(), StandardCharsets.UTF_8);
            log.setShopName(decodedShopName);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("data", synchronizationShopLogService.getPageList(log));
        map.put("total", synchronizationShopLogService.count(log));
        return map;
    }

    @GetMapping("/getListLog")
    public Map<String,Object> getListLog(SynchronizationShopLog log){
        Map<String, Object> map = new HashMap<>();
        map.put("data", synchronizationShopLogService.getList(log));
        return map;
    }

    /**
     * 新增
     */
    @PostMapping("/add")
    public Boolean save(@RequestBody SynchronizationShopLog log) {
        return synchronizationShopLogService.save(log);
    }

    /**
     * 更新
     */
    @PostMapping("/edit")
    public Boolean update(@RequestBody SynchronizationShopLog log) {
        return synchronizationShopLogService.update(log);
    }

    /**
     * 删除
     */
    @PostMapping("/deleteById/{id}")
    public Boolean delete(@PathVariable Long id) {
        return synchronizationShopLogService.deleteById(id);
    }

    /**
     * 批量删除
     */
    @PostMapping("/deleteBatch")
    public Boolean deleteBatch(@RequestBody List<Long> ids) {
        return synchronizationShopLogService.deleteBatch(ids);
    }
}