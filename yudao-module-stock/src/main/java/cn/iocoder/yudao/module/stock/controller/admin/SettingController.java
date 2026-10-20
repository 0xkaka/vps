package cn.iocoder.yudao.module.stock.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.stock.domain.Setting;
import cn.iocoder.yudao.module.stock.service.SettingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Setting管理接口。 @author vps */
@RestController
@Validated
@RequestMapping("/stock/setting")
public class SettingController {
    private final SettingService service;

    public SettingController(SettingService service) {
        this.service = service;
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('stock:setting:query')")
    public CommonResult<PageResult<Setting>> page(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(1000) int pageSize) {
        return CommonResult.success(service.page(name, pageNo, pageSize));
    }

    @PostMapping("/save")
    @PreAuthorize("@ss.hasPermission('stock:setting:edit')")
    public CommonResult<Long> save(@Valid @RequestBody Setting item) {
        service.save(item);
        return CommonResult.success(item.getId());
    }

    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('stock:setting:edit')")
    public CommonResult<Boolean> remove(@RequestParam @Positive Long id) {
        service.delete(id);
        return CommonResult.success(true);
    }
}
