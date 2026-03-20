package com.order.main.controller;

import com.order.main.entity.CourierLog;
import com.order.main.service.ICourierLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/courierLog")
public class CourierLogController {

    private final ICourierLogService courierLogService;

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public CourierLog getById(@PathVariable Long id) {
        return courierLogService.getById(id);
    }

    /**
     * 分页查询列表
     */
    @GetMapping("/getList")
    public Map getList(CourierLog courierLog) {
        Map map = new HashMap();
        map.put("data", courierLogService.getPageList(courierLog));
        map.put("total", courierLogService.count(courierLog));
        return map;
    }

    /**
     * 新增
     */
    @PostMapping("/add")
    public Boolean save(@RequestBody CourierLog courierLog) {
        return courierLogService.save(courierLog);
    }

    /**
     * 更新
     */
    @PostMapping("/edit")
    public Boolean update(@RequestBody CourierLog courierLog) {
        return courierLogService.update(courierLog);
    }

    /**
     * 删除
     */
    @PostMapping("/deleteById/{id}")
    public Boolean delete(@PathVariable Long id) {
        return courierLogService.deleteById(id);
    }



    /**
     * 批量删除
     */
    @PostMapping("/deleteBatch")
    public Boolean deleteBatch(@RequestBody List<Long> ids) {
        return courierLogService.deleteBatch(ids);
    }
}