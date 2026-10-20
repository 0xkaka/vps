package cn.iocoder.yudao.module.stock.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.stock.service.ExchangeRateService;

import jakarta.annotation.security.PermitAll;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 前台展示用汇率，不接收第三方 URL 或敏感参数。 @author vps */
@RestController
@RequestMapping("/stock")
public class PublicExchangeRateController {
    private final ExchangeRateService rates;

    public PublicExchangeRateController(ExchangeRateService rates) {
        this.rates = rates;
    }

    @PermitAll
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(enable = false)
    @GetMapping("/exchange-rates")
    public CommonResult<Map<String, Object>> current() {
        return CommonResult.success(rates.current());
    }
}
