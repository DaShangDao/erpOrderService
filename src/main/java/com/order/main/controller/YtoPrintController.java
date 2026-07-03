package com.order.main.controller;

import com.order.main.service.IYtoPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ytoPrint")
public class YtoPrintController {

    private final IYtoPrintService ytoPrintService;

}
