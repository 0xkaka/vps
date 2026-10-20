package cn.iocoder.yudao.module.stock.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.stock.service.SourceSyncService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 库存同步入口。 @author vps */
@RestController
@RequestMapping("/stock")
public class SyncController {
    private final SourceSyncService sync;

    public SyncController(SourceSyncService sync) {
        this.sync = sync;
    }

    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPermission('stock:product:edit')")
    public CommonResult<Integer> sync() {
        return CommonResult.success(sync.sync(true));
    }
}
