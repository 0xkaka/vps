package cn.iocoder.yudao.module.stock.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.stock.domain.CheckLog;
import cn.iocoder.yudao.module.stock.service.CheckLogService;

import jakarta.validation.constraints.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** CheckLog管理接口。 @author vps */
@RestController
@Validated
@RequestMapping("/stock/checklog")
public class CheckLogController {
    private final CheckLogService service;

    public CheckLogController(CheckLogService service) {
        this.service = service;
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('stock:checklog:query')")
    public CommonResult<PageResult<CheckLog>> page(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(1000) int pageSize) {
        return CommonResult.success(service.page(name, pageNo, pageSize));
    }
}
